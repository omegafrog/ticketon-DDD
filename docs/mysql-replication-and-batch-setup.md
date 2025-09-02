# MySQL 레플리케이션 및 ANALYZE 배치 시스템 구축 가이드

## 개요

이 문서는 MSA 환경에서 읽기/쓰기 분리를 통한 MySQL 레플리케이션 설정과 통계 정보 최적화를 위한 배치 시스템 구축에 대한 가이드입니다.

## 📋 구성 요소

### 1. MySQL 마스터-슬레이브 레플리케이션
- **mysql-master**: 쓰기 전용 (포트 3306)
- **mysql-replica**: 읽기 전용 (포트 3307)

### 2. 배치 시스템
- **batch 모듈**: Spring Batch 기반 ANALYZE 작업 실행
- **스케줄링**: 매주 일요일 새벽 2시 자동 실행
- **모니터링**: REST API를 통한 상태 확인

## 🚀 설정 및 실행 단계

### 1단계: 인프라 시작

```bash
# 1. Docker Compose로 MySQL 컨테이너 시작
cd docker
docker compose up -d mysql-master mysql-replica

# 2. 컨테이너가 완전히 준비될 때까지 대기 (약 30초)
docker compose logs -f mysql-master
# "ready for connections" 메시지 확인

docker compose logs -f mysql-replica
# "ready for connections" 메시지 확인
```

### 2단계: 레플리케이션 구성

```bash
# 1. 레플리케이션 자동 설정 실행
./setup-replication.sh

# 2. 레플리케이션 상태 확인
docker exec mysql-replica mysql -uroot -ppassword -e "SHOW SLAVE STATUS\G"
```

**성공적인 레플리케이션 확인 포인트:**
- `Slave_IO_Running: Yes`
- `Slave_SQL_Running: Yes`
- `Seconds_Behind_Master: 0` (또는 작은 숫자)

### 3단계: 데이터베이스 권한 설정

```bash
# 1. 마스터 DB에 권한 부여
docker exec mysql-master mysql -uroot -ppassword < mysql/grant-analyze-permissions.sql

# 2. 레플리카 DB에 권한 부여
docker exec mysql-replica mysql -uroot -ppassword < mysql/grant-analyze-permissions.sql

# 3. 권한 테스트 실행
./test-analyze-permissions.sh
```

### 4단계: 배치 애플리케이션 실행

```bash
# 1. 프로젝트 루트에서 배치 모듈 빌드
./gradlew :batch:build

# 2. 배치 애플리케이션 실행
./gradlew :batch:bootRun

# 또는 JAR 파일로 실행
java -jar batch/build/libs/batch-0.0.1-SNAPSHOT.jar
```

## 📊 모니터링 및 테스트

### API 엔드포인트

배치 애플리케이션이 실행되면 다음 API를 사용할 수 있습니다:

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/batch/health` | GET | 시스템 헬스 체크 |
| `/api/batch/analyze/run` | POST | 수동 ANALYZE 작업 실행 |
| `/api/batch/analyze/history` | GET | 배치 실행 이력 조회 |
| `/api/batch/analyze/stats` | GET | 테이블 통계 정보 조회 |

### 헬스 체크 예시

```bash
# 시스템 상태 확인
curl -X GET http://localhost:8080/api/batch/health

# 응답 예시 (성공)
{
  "status": "UP",
  "timestamp": "2024-09-02T14:30:00",
  "databases": {
    "master": {"status": "UP", "result": "OK"},
    "replica": {"status": "UP", "result": "OK"},
    "replication": {
      "status": "UP",
      "slaveIORunning": "Yes",
      "slaveSQLRunning": "Yes",
      "secondsBehindMaster": 0
    }
  }
}
```

### 수동 ANALYZE 실행 테스트

```bash
# 수동으로 ANALYZE 작업 실행
curl -X POST http://localhost:8080/api/batch/analyze/run

# 응답 예시
{
  "status": "success",
  "message": "ANALYZE statistics job started successfully",
  "timestamp": "2024-09-02T14:35:00"
}
```

## 📅 스케줄링 설정

### 기본 스케줄

- **실행 주기**: 매주 일요일 새벽 2시
- **Cron 표현식**: `0 0 2 ? * SUN`
- **대상 테이블**: events, purchases, tickets, users, seat_layouts

### 스케줄 변경

`batch/src/main/resources/application.yml`에서 설정 변경 가능:

```yaml
batch:
  analyze:
    # 매일 새벽 3시로 변경하려면
    cron: "0 0 3 * * ?"
    # 6시간마다 실행하려면  
    cron: "0 0 */6 * * ?"
```

## 🔍 레플리케이션 상태 모니터링

### 레플리케이션 지연 확인

```bash
# 레플리케이션 지연 모니터링
docker exec mysql-replica mysql -uroot -ppassword -e "
SELECT 
  IF(Seconds_Behind_Master IS NULL, 'Not Replicating', Seconds_Behind_Master) AS replication_lag,
  Slave_IO_Running,
  Slave_SQL_Running,
  Master_Log_File,
  Read_Master_Log_Pos
FROM (SHOW SLAVE STATUS) AS ss\G"
```

### 데이터 동기화 테스트

```bash
# 1. 마스터에 테스트 데이터 삽입
docker exec mysql-master mysql -uroot -ppassword -D ticketon -e "
INSERT INTO events (event_id, title, created_at) 
VALUES ('test-replication', 'Replication Test Event', NOW());"

# 2. 레플리카에서 데이터 확인 (약간의 지연 후)
sleep 2
docker exec mysql-replica mysql -uroot -ppassword -D ticketon -e "
SELECT * FROM events WHERE event_id = 'test-replication';"
```

## ⚠️ 트러블슈팅

### 레플리케이션 실패

**증상**: `Slave_IO_Running: No` 또는 `Slave_SQL_Running: No`

**해결방법**:
```bash
# 1. 에러 로그 확인
docker exec mysql-replica mysql -uroot -ppassword -e "SHOW SLAVE STATUS\G" | grep -i error

# 2. 레플리케이션 재시작
docker exec mysql-replica mysql -uroot -ppassword -e "STOP SLAVE; RESET SLAVE; START SLAVE;"

# 3. 설정 재실행
./setup-replication.sh
```

### ANALYZE 권한 오류

**증상**: `Access denied for user 'batch_analyze'@'%'`

**해결방법**:
```bash
# 권한 재부여
docker exec mysql-replica mysql -uroot -ppassword < mysql/grant-analyze-permissions.sql

# 권한 확인
docker exec mysql-replica mysql -uroot -ppassword -e "SHOW GRANTS FOR 'batch_analyze'@'%';"
```

### 배치 작업 실패

**증상**: 배치 작업이 실행되지 않거나 실패

**해결방법**:
```bash
# 1. 로그 확인
./gradlew :batch:bootRun --debug

# 2. 데이터베이스 연결 테스트
curl -X GET http://localhost:8080/api/batch/health

# 3. 수동 실행으로 테스트
curl -X POST http://localhost:8080/api/batch/analyze/run
```

## 📈 성능 모니터링

### 실행 시간 모니터링

배치 실행 로그에서 각 테이블별 ANALYZE 시간을 확인할 수 있습니다:

```
2024-09-02 02:00:15 INFO  - ANALYZE TABLE events completed successfully in 2341ms
2024-09-02 02:00:18 INFO  - ANALYZE TABLE purchases completed successfully in 1876ms
```

### 통계 정보 확인

```bash
# API를 통한 테이블 통계 확인
curl -X GET http://localhost:8080/api/batch/analyze/stats

# 직접 DB 쿼리
docker exec mysql-replica mysql -uroot -ppassword -D ticketon -e "
SELECT 
  table_name,
  table_rows,
  ROUND(data_length/1024/1024, 2) AS data_mb,
  ROUND(index_length/1024/1024, 2) AS index_mb,
  update_time
FROM information_schema.tables 
WHERE table_schema = 'ticketon';"
```

## 🎯 다음 단계

이 시스템이 구축되면 다음 단계로 진행할 수 있습니다:

1. **쿼리 힌트 시스템** - 통계 정보 기반 동적 힌트 적용
2. **성능 모니터링** - 옵티마이저 실패 패턴 감지
3. **자동 최적화** - Circuit Breaker 패턴 적용
4. **알림 시스템** - 레플리케이션 지연 또는 배치 실패 시 알림

---

**구축 완료 체크리스트:**

- [ ] MySQL 마스터-슬레이브 레플리케이션 동작 확인
- [ ] 배치 애플리케이션 정상 실행 확인  
- [ ] ANALYZE 권한 테스트 통과
- [ ] 주간 스케줄 설정 확인
- [ ] 헬스체크 API 정상 응답 확인
- [ ] 수동 ANALYZE 실행 테스트 성공