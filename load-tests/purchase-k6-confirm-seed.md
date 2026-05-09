# Purchase Confirm k6 Seed

## 목적

`payment-async-confirm.js`가 사용할 `load-tests/payment-test-data.json`을 생성한다.

이 seed는 `/api/v1/payments/init`을 호출하지 않는다. `init`은 `entryAuthToken` Redis 검증을 수행하므로, confirm 부하 테스트 데이터 준비 단계에 대기열 흐름이 섞인다. 대신 이 스크립트는 로그인 토큰을 실제로 발급받고 `purchase` 테이블에 `IN_PROGRESS` row를 직접 넣는다.

생성 흐름:

```text
/api/v1/auth/register
→ /api/v1/auth/login
→ purchase row 직접 insert
→ load-tests/payment-test-data.json 생성
→ k6 confirm 테스트
→ mock PG server가 외부 PG 역할
```

## 전제 조건

- MySQL이 실행 중이어야 한다.
- Gateway/Auth/User/Purchase/Event 관련 애플리케이션이 실행 중이어야 한다.
- mock PG 서버는 confirm 테스트 실행 전에 별도로 실행한다.
- 테스트 이벤트 초기 데이터가 필요하다.

초기 이벤트/좌석 데이터:

```bash
mysql -h 127.0.0.1 -P 3306 -uroot -ppassword ticketon \
  < load-tests/purchase-k6-initial-data.sql
```

## Seed 실행

### USER_COUNT를 직접 지정

```bash
chmod +x load-tests/seed-purchase-k6-confirm-data.sh

EVENT_ID=event-k6-001 \
USER_COUNT=1000 \
MYSQL_USER=root \
MYSQL_PASSWORD=password \
MYSQL_DATABASE=ticketon \
./load-tests/seed-purchase-k6-confirm-data.sh
```

### k6 옵션 기준으로 동적 생성

`USER_COUNT`를 생략하면 `payment-async-confirm.js`와 같은 방식으로 필요한 데이터 수를 계산한다.

계산 기준:

```text
MAX_VUS = max(VUS, RATE * 2)
ROW_BLOCK_SIZE = ROW_BLOCK_SIZE || ceil(estimatedIterations / MAX_VUS) + 10
USER_COUNT = MAX_VUS * ROW_BLOCK_SIZE
```

예시:

```bash
EVENT_ID=event-k6-001 \
VUS=1000 \
RATE=1000 \
WARMUP=3m \
DURATION=5m \
COOLDOWN=1m \
MYSQL_USER=root \
MYSQL_PASSWORD=password \
MYSQL_DATABASE=ticketon \
./load-tests/seed-purchase-k6-confirm-data.sh
```

수동으로 더 작게/크게 만들고 싶으면 `USER_COUNT`를 지정하면 된다.

## mock PG 실행

```bash
PORT=18080 PG_LATENCY_MS=1000 node load-tests/mock-pg-server.js
```

Purchase 서비스가 mock PG를 보도록 실행한다.

```bash
./gradlew :app:bootRun \
  --args='--payment.toss.api-url=http://localhost:18080/v1/payments --payment.toss.secret-key=test_secret'
```

## k6 실행

Seed script가 이미 `purchaseId`를 준비하므로 `INIT_PURCHASES=0`으로 실행한다.

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e PAYMENT_DATA_FILE=./load-tests/payment-test-data.json \
  -e INIT_PURCHASES=0 \
  -e VUS=1000 \
  -e RATE=1000 \
  -e WARMUP=3m \
  -e DURATION=5m \
  -e COOLDOWN=1m \
  load-tests/payment-async-confirm.js
```

## 주의

현재 `confirm` 컨트롤러는 `entryAuthToken` 헤더를 요구하지만 Redis 검증은 하지 않는다. 그래서 생성 데이터에는 `dummy-entry-token`을 넣는다. 이 누락 사항은 GitHub Issue #30에서 추적한다.

Issue #30이 해결되어 confirm에서도 `entryAuthToken`을 검증하게 되면, 이 seed 방식은 다음 중 하나로 수정해야 한다.

1. 대기열/entry token Redis seed를 함께 수행한다.
2. confirm test용 전용 fixture API 또는 test profile seed path를 만든다.
