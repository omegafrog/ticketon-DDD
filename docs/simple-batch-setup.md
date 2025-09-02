# 간단한 MySQL ANALYZE 배치 시스템 구축 가이드

## 🎯 개요

기존 MySQL 컨테이너에 배치 전용 사용자를 추가하여 ANALYZE 배치 작업을 실행하는 간단한 방법입니다.

## 🚀 빠른 시작

### 1단계: MySQL 시작 (이미 실행 중이면 생략)

```bash
cd docker
docker compose up -d mysql
```

### 2단계: 배치 사용자 설정

```bash
# 배치 사용자 생성 및 권한 부여
./setup-simple-batch.sh
```

### 3단계: 배치 애플리케이션 실행

```bash
# 프로젝트 루트에서
./gradlew :batch:bootRun
```

### 4단계: 테스트

```bash
# 헬스 체크
curl http://localhost:8080/api/batch/health

# 수동 ANALYZE 실행
curl -X POST http://localhost:8080/api/batch/analyze/run
```

## 🔧 주요 구성

### 배치 사용자
- **사용자명**: `batch_analyze`
- **비밀번호**: `batch_password`
- **권한**: SELECT, PROCESS, REFERENCES, INDEX

### 배치 작업
- **스케줄**: 매주 일요일 새벽 2시
- **대상 테이블**: events, purchases, tickets, users, seat_layouts
- **실행 방식**: 기존 MySQL 컨테이너 직접 사용

### API 엔드포인트
- `GET /api/batch/health` - 헬스 체크
- `POST /api/batch/analyze/run` - 수동 실행
- `GET /api/batch/analyze/history` - 실행 이력
- `GET /api/batch/analyze/stats` - 테이블 통계

## 📊 예상 응답

### 헬스 체크 성공
```json
{
  "status": "UP",
  "timestamp": "2024-09-02T15:30:00",
  "database": {
    "status": "UP",
    "result": "OK",
    "permissions": "OK"
  }
}
```

### 수동 실행 성공
```json
{
  "status": "success", 
  "message": "ANALYZE statistics job started successfully",
  "timestamp": "2024-09-02T15:35:00"
}
```

## ⚠️ 트러블슈팅

### 권한 오류
```bash
# 권한 재설정
docker exec mysql mysql -uroot -ppassword < mysql/setup-batch-user.sql
```

### 연결 실패  
```bash
# MySQL 상태 확인
docker ps | grep mysql
docker logs mysql
```

## 🎯 장점

1. **간단함**: 레플리케이션 없이 기존 DB 활용
2. **안전함**: 읽기 전용 권한으로 제한
3. **실용적**: MSA 환경에서 바로 사용 가능
4. **확장 가능**: 나중에 레플리케이션으로 업그레이드 가능

---

이 방법으로 MSA 환경에서 안전하고 간단하게 통계 정보 최적화가 가능합니다! 🚀