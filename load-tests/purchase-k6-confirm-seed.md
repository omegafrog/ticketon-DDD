# Purchase Confirm k6 Seed Data

This guide prepares deterministic data for `load-tests/payment-async-confirm.js` without calling `POST /api/v1/payments/init`.

## Why this seed path exists

`/payments/init` validates the queue entry token through Redis. For confirm performance tests, the goal is to measure the confirm accept path and async PG approval path, not the waiting queue or init path.

So this seed path:

1. Registers k6 users through the real auth API.
2. Logs in through the real auth API and stores access/refresh tokens.
3. Inserts `purchase` rows directly in `IN_PROGRESS` state.
4. Writes `load-tests/payment-test-data.json` for k6.
5. Lets k6 call only `POST /api/v1/payments/confirm`.

The confirm controller currently requires the `entryAuthToken` header, so the generated JSON includes a stable dummy value. Current confirm implementation receives that header but does not validate it. See issue #30 for the missing confirm entry-token validation follow-up.

## Prerequisites

Start infra:

```bash
docker-compose -f docker/docker-compose.yml up -d
```

Load the deterministic event/seat initial data:

```bash
mysql -h 127.0.0.1 -P 3306 -uroot -ppassword ticketon \
  < load-tests/purchase-k6-initial-data.sql
```

Start mock PG:

```bash
PORT=18080 PG_LATENCY_MS=1000 node load-tests/mock-pg-server.js
```

Run the services and point purchase PG calls to the mock server:

```bash
./gradlew :platform:eureka:bootRun
./gradlew :platform:gateway:bootRun
./gradlew :app:bootRun \
  --args='--payment.toss.api-url=http://localhost:18080/v1/payments --payment.toss.secret-key=test_secret'
```

## Generate confirm test data

Default local MySQL credentials in this repo are usually `root/password` and database `ticketon`.

```bash
chmod +x load-tests/seed-purchase-k6-confirm-data.sh

EVENT_ID=event-k6-001 \
USER_COUNT=1000 \
MYSQL_HOST=127.0.0.1 \
MYSQL_PORT=3306 \
MYSQL_USER=root \
MYSQL_PASSWORD=password \
MYSQL_DATABASE=ticketon \
./load-tests/seed-purchase-k6-confirm-data.sh
```

Output:

```text
load-tests/payment-test-data.json
```

The generated file contains one row per user/purchase:

```json
{
  "userId": "...",
  "email": "...",
  "role": "USER",
  "accessToken": "...",
  "refreshToken": "...",
  "eventId": "event-k6-001",
  "purchaseId": "...",
  "orderId": "...",
  "amount": 50000,
  "provider": "TOSS",
  "paymentKey": "...",
  "entryAuthToken": "dummy-entry-token"
}
```

## Run async confirm k6 test

Because the seed script already inserts `purchase` rows, run k6 with `INIT_PURCHASES=0`.

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e PAYMENT_DATA_FILE=./load-tests/payment-test-data.json \
  -e INIT_PURCHASES=0 \
  -e EXPECTED_CONFIRM_STATUS=202 \
  -e POLL_UNTIL_DONE=1 \
  -e STATUS_MAX_WAIT_MS=30000 \
  load-tests/payment-async-confirm.js
```

## Notes

- Re-run the seed script with a new `RUN_ID` or omit `RUN_ID` to get timestamp-based unique emails, order IDs, and purchase IDs.
- The script inserts purchases in chunks of 500 rows.
- `purchase.payment_status` is seeded as `IN_PROGRESS` so `PurchaseConfirmCommandService` can accept the confirm request.
- `expected_sales_version` is seeded as `0`, matching the `event-k6-001` initial data.
- If issue #30 is implemented later, this seed path must either generate real entry tokens or use the real queue entry flow before confirm.
