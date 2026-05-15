# Queue Impact Comparison

This comparison uses the same downstream API in both modes and only changes whether users are admitted through the broker queue first.

## Default shape

- `MODE=direct`: all authenticated users hit the target endpoint immediately.
- `MODE=queue`: all authenticated users enter `/api/v1/broker/polling/...`, wait for promotion, then hit the same target endpoint.
- Default target endpoint: `GET /api/v1/events/{eventId}/seats`

That default is intentional because it isolates the queue's traffic-smoothing effect on a real downstream service without requiring an entry token header.

## Run

Direct burst:

```bash
./k6/seed-queue-comparison-data.sh

k6 run \
  -e MODE=direct \
  -e BASE_URL=http://localhost:8080 \
  -e EVENT_IDS=event-k6-001 \
  -e VUS=1000 \
  -e USER_EMAIL_PREFIX=user \
  -e LOGIN_EMAIL_DOMAIN=example.com \
  -e DEFAULT_PASSWORD='password123!' \
  -e SUMMARY_FILE=direct-summary.json \
  k6/queue-impact-comparison.js
```

Queued burst:

```bash
k6 run \
  -e MODE=queue \
  -e BASE_URL=http://localhost:8080 \
  -e BROKER_BASE_URL=http://localhost:8080 \
  -e EVENT_IDS=event-k6-001 \
  -e VUS=1000 \
  -e USER_EMAIL_PREFIX=user \
  -e LOGIN_EMAIL_DOMAIN=example.com \
  -e DEFAULT_PASSWORD='password123!' \
  -e SUMMARY_FILE=queue-summary.json \
  k6/queue-impact-comparison.js
```

Compare the two outputs:

```bash
python3 k6/compare_queue_results.py direct-summary.json queue-summary.json
```

## Useful environment variables

- `TARGET_PATH_TEMPLATE`: default `/api/v1/events/{eventId}/seats`
- `TARGET_METHOD`: default `GET`
- `TARGET_BODY_TEMPLATE`: request body template for `POST`/`PUT`/`PATCH`
- `TARGET_CONTENT_TYPE`: default `application/json`
- `TARGET_REQUIRES_ENTRY_TOKEN`: set to `1` if the target endpoint requires `entryAuthToken`
- `USER_EMAIL_PREFIX`: default `user`, so VU 17 logs in as `user17@example.com`
- `LOGIN_BATCH_SIZE`, `LOGIN_BATCH_INTERVAL_MS`, `LOGIN_BATCH_JITTER_MS`: shape the burst
- `MAX_POLL_MS`, `MIN_POLL_AFTER_MS`, `MAX_POLL_AFTER_MS`: queue wait behavior

## What to read in the result

- `target_req_time_ms p95`: how slow the downstream API became once it was actually called
- `time_to_target_ms p95`: end-to-end user delay until the downstream API completed
- `http_req_failed rate`: transport and HTTP failure rate
- `target_success_rate`: success rate for the downstream API itself
- `queue_wait_time_ms p95`: queue-induced delay, only meaningful in `MODE=queue`

## Note on queue behavior

This script does not force the event into `CLOSED` before queue entry. The current broker implementation rejects queue entry unless the event is `OPEN`, so the comparison follows the live code path instead of relying on an outdated barrier pattern.

## Required event data

The simplest setup path is:

```bash
./k6/seed-queue-comparison-data.sh
```

That script does both:

- inserts `event-k6-001` plus its seat layout into MySQL from `load-tests/purchase-k6-initial-data.sql`
- creates the comparison users through `/api/v1/auth/register`

If your local DB credentials differ, override `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD`, or `MYSQL_DATABASE`.
