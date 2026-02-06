# Polling Queue Manual Verification

This checklist verifies the polling queue flow end-to-end without SSE.

## Preconditions

- Polling Redis is running (compose service name: `polling`).
- Polling mode run is *sequential* (do not run SSE and polling stacks concurrently).
- Broker/Dispatcher/Seat/Purchase all point to the same polling Redis.

Suggested env (examples):
- Inside docker network: `REDIS_HOST=polling REDIS_PORT=6379`
- On host: `REDIS_HOST=localhost REDIS_PORT=6381`

## API Flow

Assumptions:
- You have a valid access token (Bearer) for a USER.
- `eventIdA` is OPEN and has capacity.

### 1) Enter waiting

Request:

```bash
curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$BROKER_URL/api/v1/broker/polling/events/$eventIdA/waiting"
```

Expected:
- HTTP 200

### 2) Poll current until promoted

Request:

```bash
curl -s \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$BROKER_URL/api/v1/broker/polling/events/$eventIdA/current"
```

Expected:
- While waiting: JSON contains `state=WAITING` and `rank` and `pollAfterMs`
- After promotion: JSON contains `state=ENTRY` and `entryAuthToken`

### 3) Seat select with entryAuthToken

Request:

```bash
curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -H "entryAuthToken: $ENTRY_AUTH_TOKEN" \
  -d '{"seatList":["A-1"],"ticketCount":1}' \
  "$GATEWAY_URL/api/v1/events/$eventIdA/seats"
```

Expected:
- HTTP 200

### 4) Purchase init with entryAuthToken

Request:

```bash
curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -H "entryAuthToken: $ENTRY_AUTH_TOKEN" \
  -d '{"eventId":"'$eventIdA'","orderId":"test-order-1","amount":1000}' \
  "$GATEWAY_URL/api/v1/payments/init"
```

Expected:
- HTTP 200

### 5) Cross-event token rejection

Use the token issued for `eventIdA` against `eventIdB`.

Seat should reject:

```bash
curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -H "entryAuthToken: $ENTRY_AUTH_TOKEN" \
  -d '{"seatList":["A-1"],"ticketCount":1}' \
  "$GATEWAY_URL/api/v1/events/$eventIdB/seats"
```

Expected:
- HTTP 403 (or AccessDenied mapped error)

Purchase init should reject (eventIdB in body):

```bash
curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -H "entryAuthToken: $ENTRY_AUTH_TOKEN" \
  -d '{"eventId":"'$eventIdB'","orderId":"test-order-2","amount":1000}' \
  "$GATEWAY_URL/api/v1/payments/init"
```

Expected:
- HTTP 403 (or AccessDenied mapped error)

## Redis Observability (optional)

Keys to inspect:
- `WAITING:<eventId>`
- `WAITING_LAST_SEEN:<eventId>`
- `ENTRY_TOKEN:<userId>`
- `ENTRY_EVENT:<userId>`
- `ENTRY_LAST_SEEN`
- `ENTRY_QUEUE_SLOTS` (hash, field=`eventId`)

## Load Testing

- Do NOT run k6 scripts (including `k6/sse-throughput-test.js`) unless explicitly requested.
