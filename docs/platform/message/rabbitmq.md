docker-compose는 본래 단일 호스트용 도구이지만, 각 EC2 인스턴스에서 약간의 설정을 변경하여 실행하는 방식으로 클러스터를 구축할 수 있습니다. 핵심은 각 Kafka 컨테이너가 다른 EC2 인스턴스에 있는 동료 컨테이너를 실제 IP 주소로 찾아갈 수 있도록 설정하는 것입니다.코드 품질 리뷰 및 개선 제안현재 docker-compose.yml 파일은 로컬 테스트용 단일 노드 설정이며, 여러 EC2에 분산 환경을 구축하기에는 몇 가지 문제점이 있습니다.| 문제점 (Issue) | 설명 (Description) | 해결 방안 (Solution) | | :--- | :--- | :--- | | 단일 호스트용 설정 | 현재 설정은 localhost와 Docker 내부 네트워크 이름(kafka:19092)을 사용하고 있어, 다른 EC2 인스턴스에 있는 Kafka 노드를 찾을 수 없습니다. | 각 EC2 인스턴스의 실제 Public IP 주소를 사용하도록 설정을 변경하고, 클러스터를 구성할 모든 노드의 정보를 명시적으로 알려주도록 CONTROLLER_QUORUM_VOTERS를 수정합니다. | | 복잡하고 정적인 설정 | 주석 처리된 다중 노드 설정이 남아있어 파일이 복잡하고, NODE_ID나 IP 주소 같은 값이 파일에 하드코딩되면 각 EC2마다 파일을 수정해야 하는 번거로움이 있습니다. | 불필요한 주석을 모두 제거하고, .env 파일을 활용하여 각 EC2 인스턴스별로 달라져야 하는 값을 외부에서 주입하도록 구성합니다. 이를 통해 동일한 docker-compose.yml 파일을 모든 노드에서 재사용할 수 있습니다. | | 네트워크 분리 부재 | redis, mysql 서비스가 kafka와 같은 파일에 정의되어 있어, Kafka 클러스터만 독립적으로 관리하기 어렵습니다. | Kafka 클러스터 전용 docker-compose.yml 파일을 분리하여 관리의 명확성을 높입니다. (이 예제에서는 Kafka에 집중하여 수정합니다.) |개선된 docker-compose.yml (Kafka 클러스터용)아래는 여러 EC2 인스턴스에서 재사용할  있도록 개선된 docker-compose.yml 파일입니다.
```
# C:/Users/jiwoo/workspace/ticketon-DDD/docker/docker-compose-kafka-cluster.yml
version: '3.8'

services:
  kafka:
    image: 'bitnami/kafka:latest'
    ports:
      # 외부 클라이언트 및 다른 브로커와의 통신용 포트
      - "9092:9092"
      # 컨트롤러 간의 내부 통신용 포트
      - "9093:9093"
    environment:
      # --- .env 파일에서 주입될 변수들 ---
      # ✅ 1. 각 노드의 고유 ID (예: 1, 2, 3)
      - KAFKA_CFG_NODE_ID=${KAFKA_NODE_ID}
      # ✅ 2. 외부에 광고할 리스너. 해당 EC2의 Public IP를 사용합니다.
      - KAFKA_CFG_ADVERTISED_LISTENERS=EXTERNAL://${EC2_PUBLIC_IP}:9092
      # ✅ 3. 클러스터를 구성하는 모든 컨트롤러 노드 목록. 모든 노드가 동일한 값을 가져야 합니다.
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=${KAFKA_QUORUM_VOTERS}

      # --- 모든 노드에 동일하게 적용될 고정 설정 ---
      - KAFKA_CFG_PROCESS_ROLES=broker,controller
      - KAFKA_CFG_LISTENERS=EXTERNAL://:9092,CONTROLLER://:9093
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT
      - KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER
      - KAFKA_CFG_INTER_BROKER_LISTENER_NAME=EXTERNAL
    volumes:
      - kafka_data:/bitnami/kafka

volumes:
  kafka_data:
    name: kafka_data
```

배포 방법 (3개의 EC2 인스턴스로 클러스터 구성 예시)사전 준비: 3개의 EC2 인스턴스를 준비하고, 각 인스턴스의 Public IP를 확인합니다. (예: 1.1.1.1, 2.2.2.2, 3.3.3.3)1. EC2 보안 그룹 설정 (매우 중요)각 EC2 인스턴스의 보안 그룹에서 아래 인바운드 규칙을 추가하여 Kafka 노드끼리 서로 통신할 수 있도록 허용해야 합니다.| 타입 | 프로토콜 | 포트 범위 | 소스 | 설명 | | :--- | :--- | :--- | :--- | :--- | | 사용자 지정 TCP | TCP | 9092 | sg-xxxxxxxx | 클라이언트 및 브로커 간 통신 | | 사용자 지정 TCP | TCP | 9093 | sg-xxxxxxxx | 컨트롤러 간 통신 |•소스(Source): 3개의 EC2 인스턴스가 모두 속한 보안 그룹의 ID를 지정합니다. 이렇게 하면 해당 보안 그룹에 속한 인스턴스끼리의 통신만 허용되어 안전합니다.2. 각 EC2 인스턴스에 파일 준비위에서 수정한 docker-compose-kafka-cluster.yml 파일을 3개의 EC2 인스턴스에 모두 복사합니다.3. .env 파일 생성 및 설정각 EC2 인스턴스에서 docker-compose-kafka-cluster.yml 파일과 같은 위치에 .env 파일을 생성하고 아래 내용을 채워 넣습니다.EC2 인스턴스 1 (1.1.1.1)의 .env 파일
```
KAFKA_NODE_ID=1
EC2_PUBLIC_IP=1.1.1.1
KAFKA_QUORUM_VOTERS=1@1.1.1.1:9093,2@2.2.2.2:9093,3@3.3.3.3:9093
```
4. Kafka 클러스터 실행이제 각 EC2 인스턴스에 SSH로 접속하여 아래 명령어를 실행하면 됩니다
```
# docker-compose.yml 파일이 있는 디렉터리로 이동
cd /path/to/your/compose/file

# 백그라운드에서 컨테이너 실행
docker-compose -f docker-compose-kafka-cluster.yml up -d
```