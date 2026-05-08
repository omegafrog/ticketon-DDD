# Payment Approval Async Performance Report

## Scope

Goal: compare payment confirm before/after moving PG approval from HTTP request thread to RabbitMQ worker.

Actual repo endpoints:

| Flow | Method | Path | Status |
|---|---|---|---:|
| Init purchase | `POST` | `/api/v1/payments/init` | `201` |
| Confirm payment | `POST` | `/api/v1/payments/confirm` | sync branch: `200`, async branch: `202` |
| Poll confirm status | `GET` | `/api/v1/payments/confirm/{purchaseId}/status` | `200` |

Auth:

| Header/Cookie | Required | Notes |
|---|---:|---|
| `Authorization: Bearer <accessToken>` | yes | Gateway validates JWT, forwards `User-Id`, `Role`, `Email`. |
| `refreshToken` cookie | usually | Required by gateway if access token expired or blacklist check enabled. |
| `entryAuthToken` header | init/confirm | `init` validates token + event id. `confirm` requires header in controller signature but current async code does not validate it. |

Confirm request body:

```json
{
  "purchaseId": "purchase-id-from-init",
  "paymentKey": "mock-pg-payment-key",
  "orderId": "same-order-id-used-at-init",
  "amount": 50000,
  "provider": "TOSS"
}
```

Status response body is wrapped by `RsData`; useful fields are `data.status`, `data.paymentStatus`, `data.updatedAt`.

## Test Environment

- CPU:
- Memory:
- DB: MySQL master on `localhost:3306`
- Redis: `localhost:6379`
- RabbitMQ: `localhost:5672`, management API usually `localhost:15672`
- Spring profile:
- PG mock latency: `300ms`, `1s`, `3s`

## Required Setup

Start infra:

```bash
docker-compose -f docker/docker-compose.yml up -d
```

Start mock PG:

```bash
PORT=18080 PG_LATENCY_MS=300 node load-tests/mock-pg-server.js
```

Point purchase service at mock PG:

```bash
export PAYMENT_TOSS_API_URL=http://localhost:18080/v1/payments
export PAYMENT_TOSS_SECRET_KEY=test_secret
./gradlew :platform:eureka:bootRun
./gradlew :platform:gateway:bootRun
./gradlew :app:bootRun
```

If env binding does not override nested config in your Spring profile, pass JVM args:

```bash
./gradlew :app:bootRun --args='--payment.toss.api-url=http://localhost:18080/v1/payments --payment.toss.secret-key=test_secret'
```

Prepare deterministic test data:

1. Create enough users for desired volume.
2. Get access/refresh tokens via `POST /api/v1/auth/login`.
3. Enter queue and obtain `entryAuthToken` through broker polling/SSE flow.
4. For each user, call `POST /api/v1/payments/init` once, or set `INIT_PURCHASES=1` and let k6 call init during setup.
5. Write `load-tests/payment-test-data.json` using `payment-test-data.sample.json`.

Important repo constraint: purchase service rejects another `IN_PROGRESS` purchase for same user. Use one row per user per run, or clean/reset DB between runs.

The k6 scripts select data by VU id plus VU-local iteration to avoid reusing a purchase under `constant-arrival-rate`.
Keep enough rows for `maxVUs * ROW_BLOCK_SIZE`, or set `ALLOW_DATA_REUSE=1` only for non-mutating/status-only checks.

## Scenarios

Run same VUs, rate, duration, data volume, PG profile for before/after.

| Scenario | VUs/RPS | PG latency | Duration |
|---|---:|---:|---:|
| Smoke | 100 | 300ms, 1s, 3s | 3m warm-up + 5m measurement + 1m cool-down |
| Normal | 1,000 | 300ms, 1s, 3s | 3m warm-up + 5m measurement + 1m cool-down |
| Spike | 5,000 or constant arrival rate | 300ms, 1s, 3s | 3m warm-up + 5m measurement + 1m cool-down |

Example sync baseline against old/sync branch:

```bash
PORT=18080 PG_LATENCY_MS=1000 node load-tests/mock-pg-server.js
BASE_URL=http://localhost:8080 \
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e PAYMENT_DATA_FILE=/home/jiwoo/workspace/ticketon-DDD/load-tests/payment-test-data.json \
  -e VUS=1000 -e RATE=1000 -e WARMUP=3m -e DURATION=5m -e COOLDOWN=1m \
  -e EXPECTED_CONFIRM_STATUS=200 \
  -e SUMMARY_FILE=payment-sync-1s-summary.json \
  load-tests/payment-sync-confirm.js
```

Example async run against current branch:

```bash
PORT=18080 PG_LATENCY_MS=1000 node load-tests/mock-pg-server.js
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e PAYMENT_DATA_FILE=/home/jiwoo/workspace/ticketon-DDD/load-tests/payment-test-data.json \
  -e VUS=1000 -e RATE=1000 -e WARMUP=3m -e DURATION=5m -e COOLDOWN=1m \
  -e EXPECTED_CONFIRM_STATUS=202 -e POLL_UNTIL_DONE=1 -e STATUS_MAX_WAIT_MS=30000 \
  -e SUMMARY_FILE=payment-async-1s-summary.json \
  load-tests/payment-async-confirm.js
```

Status polling only:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e PAYMENT_DATA_FILE=/home/jiwoo/workspace/ticketon-DDD/load-tests/payment-test-data.json \
  -e VUS=1000 -e RATE=1000 -e DURATION=5m \
  load-tests/payment-status-polling.js
```

## Metrics Collection

Spring Actuator Prometheus:

```bash
curl -s http://localhost:9000/actuator/prometheus > app-prometheus.txt
curl -s http://localhost:9003/actuator/prometheus > purchase-prometheus.txt
curl -s http://localhost:8080/actuator/prometheus > gateway-prometheus.txt
curl -s http://localhost:18080/metrics > mock-pg-prometheus.txt
```

RabbitMQ management API:

```bash
curl -su root:root http://localhost:15672/api/queues/%2F/payment.confirm.queue > rabbit-payment-confirm-queue.json
```

Useful RabbitMQ fields: `messages_ready`, `messages_unacknowledged`, `message_stats.publish_details.rate`, `message_stats.deliver_get_details.rate`, `message_stats.ack_details.rate`.

Outbox pending count:

```bash
mysql -h 127.0.0.1 -P 3306 -uticketon -pticketon ticketon \
  -e "select count(*) as pending from purchase_outbox where published_at is null;"
```

Local docker default in this repo may be `root/password`:

```bash
mysql -h 127.0.0.1 -P 3306 -uroot -ppassword ticketon \
  -e "select count(*) as pending from purchase_outbox where published_at is null;"
```

Confirm status counts:

```bash
mysql -h 127.0.0.1 -P 3306 -uticketon -pticketon ticketon \
  -e "select status, count(*) from purchase_confirm_status group by status;"
```

Completion latency approximation from DB:

```bash
mysql -h 127.0.0.1 -P 3306 -uticketon -pticketon ticketon \
  -e "select status, timestampdiff(microsecond, min(updated_at), max(updated_at))/1000 as status_window_ms from purchase_confirm_status group by status;"
```

Better completion latency requires event table timestamps or custom metrics. See "Missing Metrics".

## Prometheus Queries

HTTP response time:

```promql
histogram_quantile(0.95, sum by (le, uri, method) (rate(http_server_requests_seconds_bucket{uri="/api/v1/payments/confirm"}[5m])))
histogram_quantile(0.99, sum by (le, uri, method) (rate(http_server_requests_seconds_bucket{uri="/api/v1/payments/confirm"}[5m])))
sum(rate(http_server_requests_seconds_count{uri="/api/v1/payments/confirm"}[5m]))
sum(rate(http_server_requests_seconds_count{uri="/api/v1/payments/confirm",status=~"5..|4.."}[5m])) / sum(rate(http_server_requests_seconds_count{uri="/api/v1/payments/confirm"}[5m]))
```

Tomcat busy threads:

```promql
tomcat_threads_busy_threads
tomcat_threads_config_max_threads
tomcat_threads_busy_threads / tomcat_threads_config_max_threads
```

Hikari DB pool:

```promql
hikaricp_connections_active
hikaricp_connections_max
hikaricp_connections_active / hikaricp_connections_max
```

Mock PG latency:

```promql
histogram_quantile(0.95, rate(mock_pg_latency_ms_bucket[5m]))
rate(mock_pg_confirm_requests_total[5m])
rate(mock_pg_errors_total[5m])
```

Existing purchase gauges:

```promql
ticketon_payment_status_count
```

## Missing Metrics To Add

Current repo exposes Actuator/Prometheus and payment status gauges, but lacks enough custom async metrics for final proof.

Minimal Micrometer metrics:

| Metric | Type | Where |
|---|---|---|
| `ticketon.payment.confirm.accept.latency` | timer | `PurchaseConfirmCommandService.requestConfirm` |
| `ticketon.payment.confirm.outbox.pending` | gauge | count `purchase_outbox where published_at is null` |
| `ticketon.payment.confirm.outbox.publish` | counter/timer | `RabbitPurchaseConfirmMessagePublisher.publish` |
| `ticketon.payment.confirm.worker.duration` | timer | `PurchaseConfirmWorker.process` around PG + finalization |
| `ticketon.payment.pg.confirm.latency` | timer | `TossPaymentPgApiService.confirmPayment` |
| `ticketon.payment.confirm.accepted_to_done` | timer | accepted timestamp from payload/outbox to `DONE` projection |
| `ticketon.payment.confirm.e2e` | timer | confirm HTTP accepted timestamp to final status visible |

RabbitMQ metrics can come from RabbitMQ Prometheus plugin or management API. If plugin enabled:

```promql
rabbitmq_queue_messages_ready{queue="payment.confirm.queue"}
rate(rabbitmq_queue_messages_published_total{queue="payment.confirm.queue"}[5m])
rate(rabbitmq_queue_messages_delivered_total{queue="payment.confirm.queue"}[5m])
rate(rabbitmq_queue_messages_acked_total{queue="payment.confirm.queue"}[5m])
```

## Required Output Tables

Before: Synchronous PG Approval

| Metric | Value |
|---|---:|
| confirm API p50/p95/p99 | from `payment-sync-summary.json` |
| confirm API throughput | `confirm_api_requests.rate` |
| error rate | `confirm_api_error_rate.rate` |
| timeout rate | `confirm_api_timeout_rate.rate` |
| Tomcat busy/max | Prometheus |
| DB active/max | Prometheus |
| PG p95/p99 | mock PG `/metrics` |

After: RabbitMQ Async Approval

| Metric | Value |
|---|---:|
| confirm API p50/p95/p99 | from `payment-async-summary.json` |
| confirm API throughput | `confirm_api_requests.rate` |
| error rate | `confirm_api_error_rate.rate` |
| timeout rate | `confirm_api_timeout_rate.rate` |
| queue depth | RabbitMQ API/Prometheus |
| publish/consume rate | RabbitMQ API/Prometheus |
| outbox pending count | DB query/custom gauge |
| accepted-to-done p95 | `accepted_to_done_latency_ms` |
| e2e p95 | `end_to_end_latency_ms` |
| PG p95/p99 | mock PG `/metrics` |

Quantitative Improvement

| Metric | Before | After | Improvement |
|---|---:|---:|---:|
| Confirm p95 latency | `B` | `A` | `(B - A) / B * 100` reduction |
| Confirm throughput | `B` | `A` | `(A - B) / B * 100` increase |
| Web occupation p95 | `B` | `A` | `(B - A) / B * 100` reduction |
| Final completion p95 | `B` | `A` | Async may be slower under queue backlog |

Do not claim improvement until numbers are filled from same env, same PG latency, same VUs/RPS, same data volume.

## Interpretation Checklist

- Confirm API latency improvement: compare HTTP acceptance only.
- Web resource occupation reduction: use confirm request duration + Tomcat busy/max.
- Throughput improvement: compare measured `confirm_api_requests.rate` at stable error rate.
- New bottleneck: inspect RabbitMQ depth, worker consume rate, DB active conns, PG latency.
- Reliability trade-off: async returns fast, but final `DONE` may lag or fail later. Client must poll status.
