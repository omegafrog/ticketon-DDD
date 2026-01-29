# Fix Plan: k6 adminController stuck (barrier never completes)

## Context

`k6/sse-throughput-test.js` has an `admin_controller_scenario` that runs `adminController()` once.
If `adminController()` does not return, the whole k6 run does not finish.

Note on the "VU count mismatch": the script defines two scenarios:
- `main_scenario` uses `vus: DEFAULT_VUS` (e.g., 1000)
- `admin_controller_scenario` uses `vus: 1`

So k6 may report 1001 total VUs created. That's expected and separate from the 1000 user VUs.

Also: the `adminController()` barrier does not depend on `redisClient.get()`.
The only `redisClient.get()` shown in this script is in the commented-out "promotion timing" block inside the SSE event handler; it is unrelated to the barrier logic.

Observed symptom pattern (from the sample output):
- `main_scenario` finishes quickly (e.g., ~9s)
- but the overall run stays alive for ~`BARRIER_TIMEOUT_MS` (default 300000ms = 5m)
- `WAITING:*` can be empty and `completed_users == DEFAULT_VUS` still holds

This strongly suggests `adminController()` is waiting on a condition that can remain false even when all VUs have already progressed and cleaned up.

Most likely cause: `adminController()` waits for `totalWaiting >= DEFAULT_VUS` computed via `ZCARD(WAITING:{eventId})` totals, but `WAITING:{eventId}` is a live queue that shrinks due to promotion/cleanup.

Per `docs/dispatcher/waitingqueue.md`, dispatcher promotion removes users from:
- `WAITING:{eventId}` (ZSET)
- `WAITING_QUEUE_INDEX_RECORD:{eventId}` (HASH)
- `WAITING_USER_IDS:{eventId}` (HASH)

So even if every VU successfully connects and later receives `IN_PROGRESS` (and is removed), `totalWaiting` may never reach `DEFAULT_VUS` *at the same time*.

Decision confirmed: barrier not met => fail-fast (terminate with a clear error).

## Goal

Ensure `adminController()` always terminates deterministically without relying on volatile queue length. When the run is incomplete, fail-fast with actionable diagnostics.

Constraint confirmed: do not call `redisClient.get()` in k6.

## Strategy

Keep the barrier *meaning* ("wait until all user VUs have reached the waiting SSE endpoint") but replace the proxy metric.

Instead of relying on the concurrent size of Redis queue keys (which can shrink), introduce a run-scoped, monotonic barrier marker written by each VU so `adminController()` can wait on a stable count (independent of Redis queue cleanup).

- `K6_BARRIER_JOINED:{runId}` (Redis SET): each VU adds its `userId` once the SSE initial handshake is verified OK.
- Track failures (optional but recommended for diagnosis):
  - `K6_BARRIER_LOGIN_FAIL:{runId}` (counter)
  - `K6_BARRIER_SSE_FAIL:{runId}` (counter)

Important constraint (k6 x/redis): avoid `await redisClient.get()`.
It's OK to read values via `redisClient.sendCommand('GET', key)`.

Barrier accounting:

`accounted = SCARD(K6_BARRIER_JOINED:{runId}) + loginFail + sseFail`

When `accounted >= DEFAULT_VUS`:
- if failures > 0 => fail-fast with counts + `getWaitingCountsByEvent()`
- else => proceed to OPEN events (if that step is still desired) and return

This avoids the failure mode where `WAITING:*` becomes empty (all users promoted/cleaned up) but the barrier condition is never met because it was based on concurrent queue size.

Additionally, ensure the barrier wait metric is recorded even on failure (so `barrier_wait_ms` doesn't silently read as 0 / missing when a timeout occurs).

## TODOs

- [x] 1) Add run-scoped Redis keys for barrier accounting

  What to do:
  - Add key builders (by `runId`):
    - `K6_BARRIER_JOINED:{runId}` (SET)
    - `K6_BARRIER_LOGIN_FAIL:{runId}` (counter)
    - `K6_BARRIER_SSE_FAIL:{runId}` (counter)
  - (Recommended) set TTL for these keys so they don't accumulate.
  - Use `sendCommand` for Redis ops:
    - `SADD`, `SCARD`, `INCR`, `GET`, `EXPIRE`

  References:
  - `k6/sse-throughput-test.js` (existing `buildOpenAtKey()` pattern for run-scoped keys)

  Acceptance:
  - Keys exist with expected types during a run.

- [x] 2) Record VU login final failure to Redis before aborting

  What to do:
  - In `export default function (data)`:
    - If `loginUserWithRetry()` returns null, `INCR K6_BARRIER_LOGIN_FAIL:{runId}` then `fail('login failed')`.

  Acceptance:
  - With intentionally-bad credentials, the counter increments and the run fails fast (no hang).

- [x] 3) Record SSE initial connect failures to Redis

  What to do:
  - If SSE initial connect check fails (non-200), `INCR K6_BARRIER_SSE_FAIL:{runId}` then fail-fast.

  References:
  - `k6/sse-throughput-test.js` (`connectSse()` initial `checkOk`)

  Acceptance:
  - When broker returns non-200 for SSE connect, the counter increments and the run fails fast.

- [x] 4) Record barrier join from each VU on successful SSE handshake

  What to do:
  - After SSE handshake is verified OK, `SADD K6_BARRIER_JOINED:{runId} {userId}`.
  - Use `{userId} = __VU` (or the same identifier used for email generation) so it is deterministic.

  Acceptance:
  - `SCARD K6_BARRIER_JOINED:{runId}` reaches `DEFAULT_VUS` in a success run even if `ZCARD WAITING:*` drops.

- [x] 5) Update `adminController()` barrier logic to use barrier keys

  What to do:
  - Replace current `totalWaiting >= DEFAULT_VUS` condition with:
    - `joined = SCARD K6_BARRIER_JOINED:{runId}` (via `sendCommand('SCARD', key)`)
    - `loginFail = GET K6_BARRIER_LOGIN_FAIL:{runId}` (via `sendCommand('GET', key)`)
    - `sseFail = GET K6_BARRIER_SSE_FAIL:{runId}` (via `sendCommand('GET', key)`)
    - `accounted = joined + loginFail + sseFail`
  - If `accounted >= DEFAULT_VUS`:
    - if failures > 0: `fail()` with counts + `getWaitingCountsByEvent()`
    - else: proceed to OPEN events and return
  - Keep `BARRIER_TIMEOUT_MS` as a hard stop with detailed failure output.
  - Use `try/finally` so `barrier_wait_ms` is always recorded (success or timeout fail).

  Acceptance:
  - In the "VU churns out of WAITING" case, `adminController()` still exits quickly (no hang).

- [x] 6) Manual verification steps

  Commands (examples):
  - Start infra: `docker-compose -f docker/docker-compose.yml up -d`
  - Run k6 with small VUs:
    - `k6 run -e EVENT_IDS=... -e VUS=10 -e SCENARIO=custom k6/sse-throughput-test.js`
  - Inspect barrier keys:
    - `redis-cli SCARD K6_BARRIER_JOINED:{runId}`
    - `redis-cli GET K6_BARRIER_LOGIN_FAIL:{runId}`
    - `redis-cli GET K6_BARRIER_SSE_FAIL:{runId}`

  Expected:
  - Success run: `adminController()` prints “Barrier met … Setting events to OPEN” and exits.
  - Failure run: `adminController()` fails with message including `joined/loginFail/sseFail` and byEvent counts.
