# 게이트웨이
## 도입 이유
인증/인가를 모든 모듈에 공통적으로 넣어야 함으로 인해서 실질적으로 spring security 의존성을 모두가 가지게 되었다.<br/>
이는 단일 책임 원칙을 벗어나고 인증이 횡단 관심사이므로 공통 부분에서 관리하는 것이 좋다고 판단.
## 구조
clinet -> gateway -> gateway filter -> validate token / decode token -> api route

## 구현 과정
### 모듈 추가
* 새로운 패키지를 추가하는 이유
  * spring web mvc는 blocking 모델이라서 요청이 시작되면 하나의 스레드가 할당되고 요청이 종료되기 전까지 스레드를 점유한다.
  * 내부적으로 tomcat을 이용한다.
  * 이는 대규모 처리에 부적합
  * 반면 spring cloud gateway는 nonblocking 모델을 사용해 요청을 처리한다.
  * 내부적으로 Netty를 사용한다.
* blocking vs nonblocking
  * 처리 유닛(스레드)가 작업의 처리 완료까지 대기하는지의 여부
  * 호출 함수가 스레드의 제어권을 돌려주는지 여부
* syncronous vs asyncronous
  * 호출의 요청의 순서가 응답에도 보장되는 것
  * 함수 호출의 요청과 결과가 한 순서로 일어남 vs 별개의 순서로 일어남
  * 요청 순서가 1->2->3->4라면 응답 순서도 1->2->3->4이다
* 
* 스레드 vs 프로세스
  * 프로세스
    * 실행중인 하나의 프로그램
    * 자신만의 메모리를 가짐
    * 다른 프로세스의 메모리 접근 불가
  * 스레드
    * 프로세스 내부에서 작업하는 최소 단위
    * 프로세스의 메모리 및 자원 공유
### 의존성 추가
```groovy
// (기존 Spring Boot 의존성은 생략)
dependencies {
    // Spring Cloud Gateway 의존성 추가
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    
    // Spring Cloud의 버전 관리를 위해 필요합니다.
    // 프로젝트 최상단 build.gradle에 dependencyManagement 블록으로 관리하는 것이 더 좋습니다.
}
```

### 게이트웨이 라우팅 설정 파일 (`application.yml`)
```yml
server:
  port: 8080 # 게이트웨이는 8080 포트에서 실행됩니다.

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        # User 서비스로 가는 라우팅 규칙
        - id: user-service-route # 규칙의 고유 ID
          uri: http://localhost:8081 # user 모듈이 실행될 주소
          predicates:
            - Path=/api/v1/users/** # 이 경로 패턴의 요청이 오면 위 uri로 전달합니다.

        # Event 서비스로 가는 라우팅 규칙
        - id: event-service-route
          uri: http://localhost:8082 # event 모듈이 실행될 주소
          predicates:
            - Path=/api/v1/events/**

# 각 도메인 모듈(user, event)은 application.yml 또는 properties에 
# server.port=8081, server.port=8082 와 같이 각각 다른 포트를 지정해야 합니다.
```
### 게이트웨이 애플리케이션 클래스 생성
```java
package org.codenbug.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
```

### 게이트웨이 역할
* 인가 토큰 검증
* 토큰 decode 및 헤더 설정 ( userId, role )

# nginx vs spring cloud
1. Nginx (또는 AWS의 ALB 같은 클라우드 로드밸런서)의 역할
- 최전방 방어벽: 인터넷의 모든 트래픽을 가장 먼저 받습니다.
- SSL/TLS 종료(Termination): https:// 요청을 처리하여 내부망으로 들어갈 때는 암호화되지 않은 HTTP로 변환합니다. 내부 서버들이 복잡한 SSL 처리를 신경 쓰지 않게 해줍니다.
- API Gateway 로드밸런싱: 여러 개로 스케일 아웃된 Spring Cloud Gateway 서버들에게 트래픽을 1차적으로 분산합니다. (Gateway 자체의 고가용성 확보)
- 정적 콘텐츠 서빙: 이미지, JS, CSS 파일 등은 마이크로서비스까지 요청을 보내지 않고 Nginx가 직접 빠르게 응답합니다.
2. Spring Cloud Gateway의 역할 
- 지능적인 문지기: Nginx로부터 "안전하게 걸러진" 트래픽을 받습니다.
- 인증/인가: 요청 헤더의 JWT 토큰이 유효한지, 해당 사용자가 이 API를 호출할 권한이 있는지 등을 확인합니다.
- 동적 라우팅: 유레카에 등록된 서비스들의 실시간 상태를 확인하고, 가장 적절한 마이크로서비스 인스턴스로 요청을 전달합니다. (예: lb://broker-service)
- API 정책 적용: 특정 API에 대한 요청 횟수 제한(Rate Limiting), 데이터 형식 변환 등 세밀한 제어를 수행합니다.요약

Nginx: "어떤 서버로 보낼까?"를 IP 주소를 보고 결정하는, 빠르고 강력한 교통 경찰.

Spring Cloud Gateway: "이 요청은 어떤 서비스로 보내야 할까?"를 서비스 이름과 비즈니스 규칙을 보고 결정하는, 똑똑하고 섬세한 안내 데스크.