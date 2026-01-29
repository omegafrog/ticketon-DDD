## 2026-01-28 Task: init
- 

## 2026-01-28 Task: barrier keys
- Added run-scoped Redis barrier keys in `k6/sse-throughput-test.js`: `K6_BARRIER_JOINED:{runId}` (SET), `K6_BARRIER_LOGIN_FAIL:{runId}` (counter), `K6_BARRIER_SSE_FAIL:{runId}` (counter).
- TTL: `K6_BARRIER_TTL_SECONDS` env override (default 3600); best-effort `EXPIRE` via `redisClient.sendCommand`.
- adminController barrier now uses `SCARD(joined) + GET(loginFail) + GET(sseFail)` rather than volatile `WAITING:*` size.

## 2026-01-28 Task: rollback
- Reverted `k6/sse-throughput-test.js` back to the previous WAITING-based barrier implementation (removed K6_BARRIER_* keys and async changes) because the new barrier approach did not work in your environment.
