# k6 Waiting Queue: HASH vs ZSET Branch Comparison

## Context

### Original Request
Compare two branches using `k6/sse-throughput-test.js` to measure waiting-queue load and qualitatively evaluate performance differences.

### Branches
- `main` (WAITING is ZSET)
- `benchmark/hash-waiting-queue` (WAITING is HASH)

### Key Architectural Difference (what we are actually testing)
`WAITING:{eventId}` implementation differs:
- `main`: ZSET-based ordering; dispatcher promotion uses Lua windowing.
- `benchmark/hash-waiting-queue`: HASH(userId -> idx) + full scan + sort for ordering; dispatcher promotion is Java-side scan/sort and per-user ops (non-atomic).

Code references for the difference:
- `broker/src/main/java/org/codenbug/broker/infra/WaitingQueueRedisRepository.java` (ZADD vs HSET for WAITING)
- `broker/src/main/java/org/codenbug/broker/infra/QueueInfoScheduler.java` (ZRANK vs HGETALL+sort)
- `broker/src/main/java/org/codenbug/broker/service/SseEmitterService.java` (ZREM vs HDEL on cleanup)
- `dispatcher/src/main/java/org/codenbug/messagedispatcher/thread/EntryPromoter.java` (Lua vs HGETALL+sort)
- `dispatcher/src/main/resources/promote_all_waiting_for_event.lua` (main only)

### Test Inputs (fixed)
- `BASE_URL=http://localhost:8080`
- `BROKER_BASE_URL=http://localhost:9000`
- `LOGIN_EMAIL_DOMAIN=example.com`
- Admin control (for PATCH event status): `ADMIN_EMAIL=admin@example.com`, `ADMIN_PASSWORD=password123`

### Event IDs
Provided:
- `0dc2efd8-f760-11f0-8e3b-2a897f646219`
- `b55834d0-f834-11f0-999b-a7e49b723099`
- `ba1d5e02-f834-11f0-999b-05d0f9a25396`
- `bc113ec4-f834-11f0-999b-1786beee8658`
- `be54c626-f834-11f0-999b-9fdbde76a760`

Use:
- K=1: `0dc2efd8-f760-11f0-8e3b-2a897f646219`
- K=4: `b55834d0-f834-11f0-999b-a7e49b723099,ba1d5e02-f834-11f0-999b-05d0f9a25396,bc113ec4-f834-11f0-999b-1786beee8658,be54c626-f834-11f0-999b-9fdbde76a760`

### Seat/slot configurations (we will run both)
- Seat-limited (default): keep event seat count as-is (currently seat=50), so `ENTRY_QUEUE_SLOTS[eventId]` stays small.
- Slot-boosted (benchmark): pre-set `ENTRY_QUEUE_SLOTS[eventId]=1000` in Redis before the run.

Why this matters:
- Seat-limited highlights broker SSE broadcast stability (many users remain waiting longer).
- Slot-boosted removes the slot bottleneck so WAITING structure + promotion implementation cost dominates.

---

## Work Objectives

### Core Objective
Produce a fair, reproducible, qualitative comparison of waiting-queue performance between `main` and `benchmark/hash-waiting-queue` using the same k6 harness and controlled conditions.

### Concrete Deliverables
- A runnable benchmarking harness (single k6 script used for both branches).
- Raw results per run (stdout + JSON) saved under a consistent folder structure.
- A short comparison report with:
  - key metrics per run cell
  - interpretation (what changed and why)
  - correctness/validity notes (environment limits, failure modes)

### Definition of Done
- Both branches complete all planned runs without test-harness failures.
- Results include, per run, at minimum:
  - success rate (`completed_users / VUS`)
  - `sse_connection_time p(95)`
  - `barrier_wait_ms max` (if barrier enabled)
  - failure counters (login/sse/disconnect)
- Report includes a clear conclusion for:
  - seat-limited vs slot-boosted differences
  - K=1 vs K=4 differences

### Must NOT Have (guardrails)
- Do not change application code to “optimize” either branch as part of this comparison.
- Do not use different k6 scripts per branch (unless explicitly labeled as a different experiment).
- Do not reuse Redis queue keys across branch runs without cleanup (prevents ZSET/HASH WRONGTYPE contamination).

---

## Verification Strategy (Manual / Benchmark)

### Preflight checks (environment)
- Verify k6 supports extensions used by the harness:
  - `k6/x/sse`
  - `k6/x/redis`
- If extensions are missing, build or obtain an xk6 binary that includes them (document the exact binary used so results are reproducible).
- Verify OS limits are compatible with 10k SSE connections:
  - file descriptors: `ulimit -n`
  - (optional) ephemeral ports / conntrack if using Docker networking

### System under test
Run the same set of services for each branch (typical minimum):
- `gateway` (for `/api/v1/auth/login`, `/api/v1/events/...`)
- `broker` (SSE endpoints)
- `dispatcher` (promotion engine)
- infra: Redis + DBs via `docker/docker-compose.yml` as needed

Reference commands (from repo guidelines): `./gradlew :<module>:bootRun`.

---

## Task Flow

1) Prepare a branch-agnostic k6 harness
2) Define run matrix + run ordering
3) Preflight environment + services
4) For each run cell:
   - hard-clean Redis keys
   - set slot config (seat-limited vs slot-boosted)
   - run k6
   - capture results
5) Summarize into a report with qualitative conclusions

---

## TODOs

### 0. Create a single k6 harness usable on BOTH branches

**What to do**:
- Create a stable copy of the k6 script that will NOT change when checking out branches.
  - Recommended location: `.sisyphus/bench/sse-throughput-test.js` (or another non-git-checked-out path).
- Base it on the richer harness currently at `k6/sse-throughput-test.js` (the one that includes:
  - admin scenario (CLOSED -> barrier -> OPEN)
  - Redis precondition checks
  - barrier via Redis waiting counts
  - detailed counters/trends
- Modify ONLY the Redis WAITING counting logic to support both WAITING implementations:
  - If `TYPE WAITING:{eventId}` is `zset` -> use `ZCARD`.
  - If `TYPE WAITING:{eventId}` is `hash` -> use `HLEN`.
  - If `none` -> treat as 0.
  - Otherwise -> fail with clear error.
- Apply this type-aware logic in:
  - setup precondition check
  - `getWaitingTotal()`
  - `getWaitingCountsByEvent()`

**References**:
- `k6/sse-throughput-test.js` (current harness and metrics)
- `broker/src/main/java/org/codenbug/broker/infra/WaitingQueueRedisRepository.java` (WAITING is ZSET vs HASH)
- `broker/src/main/java/org/codenbug/broker/service/SseEmitterService.java` (cleanup differs per branch)

**Acceptance Criteria**:
- Running `k6 run .sisyphus/bench/sse-throughput-test.js` successfully imports `k6/x/sse` and `k6/x/redis`.
- For a zset WAITING branch and a hash WAITING branch, the harness barrier logic does not crash with WRONGTYPE.

### 1. Define the run matrix and storage layout

**What to do**:
- Use 16 runs total:
  - Branches: 2 (`main`, `benchmark/hash-waiting-queue`)
  - K: 2 (K=1, K=4)
  - Slot mode: 2 (seat-limited, slot-boosted)
  - Scenario: 2 (`SCENARIO=baseline`, `SCENARIO=target`)
- Randomize or alternate run order to reduce “warm machine” bias.
- Create a results directory convention, e.g.:
  - `.sisyphus/evidence/k6/{timestamp}/{branch}/{K}/{slotMode}/{scenario}/`
  - Save:
    - stdout summary
    - `summary.json`
    - service logs snapshots (broker/dispatcher)

**Acceptance Criteria**:
- Every run produces a unique folder containing stdout + summary JSON.

### 2. Preflight: verify services and auth assumptions

**What to do**:
- Confirm admin PATCH endpoint is reachable through gateway:
  - `/api/v1/events/{eventId}?status=OPEN|CLOSED`
  - Controller reference: `event/src/main/java/org/codenbug/event/ui/EventCommandController.java`
- Confirm broker SSE endpoint is reachable:
  - `GET {BROKER_BASE_URL}/api/v1/broker/events/{eventId}/tickets/waiting`
  - `POST {BROKER_BASE_URL}/api/v1/broker/events/{eventId}/tickets/disconnect`
  - Controller reference: `broker/src/main/java/org/codenbug/broker/ui/WaitingQueueController.java`
- Confirm load-test users exist for the chosen VU range (user already confirmed).

**Acceptance Criteria**:
- A single VU run completes successfully on each branch (smoke test).

### 3. Hard-clean Redis keys between EVERY run

**What to do**:
- For each eventId in the run’s EVENT_IDS, delete queue keys to avoid:
  - leftover state
  - ZSET/HASH WRONGTYPE cross-branch collisions
- Minimum per-event deletion:
  - `DEL WAITING:{eventId}`
  - `DEL WAITING_USER_IDS:{eventId}`
  - `DEL WAITING_QUEUE_INDEX_RECORD:{eventId}`
  - `HDEL WAITING_QUEUE_IDX {eventId}` (if present)
  - `HDEL ENTRY_QUEUE_SLOTS {eventId}` (only if you want seat-limited init)
- Also clear harness keys:
  - `DEL K6_OPEN_AT_MS:{runId}` (or delete by prefix if you keep multiple)

**References**:
- Key naming and meaning: `docs/dispatcher/waitingqueue.md`, `docs/broker/Waiting_queue.md`
- Slot semantics: `docs/troubleshooting/entry-queue-count-meaning.md`

**Acceptance Criteria**:
- Immediately before each k6 run, `TYPE WAITING:{eventId}` returns `none`.
- Setup precondition in k6 does not fail.

### 4. Slot configuration per run

**Seat-limited**:
- Ensure `ENTRY_QUEUE_SLOTS[eventId]` is NOT preset so broker initializes it from event seat count.

**Slot-boosted**:
- Pre-set slots for each eventId:
  - `HSET ENTRY_QUEUE_SLOTS {eventId} 1000`
- Verify by readback.

**Acceptance Criteria**:
- Seat-limited: `ENTRY_QUEUE_SLOTS[eventId]` matches expected low value (e.g., 50) after first entries.
- Slot-boosted: `ENTRY_QUEUE_SLOTS[eventId] == 1000` before starting k6.

### 5. Execute the 16-run matrix (baseline + target)

**What to do**:
- For each run cell, execute `k6 run` with env vars:
  - `BASE_URL`, `BROKER_BASE_URL`, `LOGIN_EMAIL_DOMAIN`
  - `ADMIN_EMAIL`, `ADMIN_PASSWORD`
  - `REDIS_URL` (default in script is `redis://127.0.0.1:6379`)
  - `SCENARIO` in `{baseline,target}`
  - `EVENT_IDS` set to K=1 or K=4 list
  - Optional tuning kept fixed across branches:
    - `LOGIN_BATCH_SIZE`, `LOGIN_BATCH_INTERVAL_MS`, `LOGIN_BATCH_JITTER_MS`
    - `BARRIER_TIMEOUT_MS`
- Capture stdout and `summary.json` produced by `handleSummary`.

**Acceptance Criteria**:
- For each run:
  - `completed_users == VUS` OR failures are clearly attributable (auth, environment limits, SSE failures) and recorded.
  - `http_req_failed` stays below threshold (or threshold adjusted intentionally and noted).

### 6. Analyze and write the qualitative comparison report

**What to do**:
- Produce a single short report (recommended location: `docs/platform/common/api_load_assessment.md` or a new doc under `docs/`), including:
  - a table of metrics for each of the 16 runs
  - a narrative section:
    - Seat-limited findings: broker SSE stability, queue broadcast behavior
    - Slot-boosted findings: promotion throughput + correctness risks
    - K=1 vs K=4 findings: single hot event vs distribution
- Highlight expected causal links based on code:
  - Hash branch does `HGETALL+sort` in `QueueInfoScheduler` and `EntryPromoter`.
  - Main branch uses ZSET operations and Lua windowing (less round trips, atomic).

**Acceptance Criteria**:
- Report states:
  - which metrics improved/regressed
  - whether differences are environment-limited (FDs, CPU) vs implementation-limited
  - any correctness anomalies (e.g., lower completion rate, unexpected disconnect failures)

---

## Notes (from Metis review)
- Keep k6 harness identical across branches; avoid mixing in branch-local versions of `k6/sse-throughput-test.js`.
- Ensure Redis key cleanup between runs to avoid ZSET/HASH `WRONGTYPE` errors.
- 10k SSE connections can be OS-limited; record `ulimit -n` and machine specs alongside results.
- Treat this as “as-implemented end-to-end” comparison: hash branch changes atomicity and work pattern, not just data structure.
