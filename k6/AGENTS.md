# Load Testing (k6/)

`k6/` contains k6 load test scripts.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| SSE throughput test | `k6/sse-throughput-test.js` | Uses gateway/broker APIs and Redis |

## Key Environment Vars
| Var | Default | Notes |
|-----|---------|------|
| `BASE_URL` | `http://localhost:8080` | Gateway base URL |
| `BROKER_BASE_URL` | `BASE_URL` | Broker endpoint base |
| `REDIS_URL` | `redis://127.0.0.1:6379` | For k6 redis extension |
| `EVENT_IDS` | (none) | Comma-separated IDs |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | (none) | Used by setup/admin scenario |
| `SCENARIO` | (none) | Baseline/target/stress style switches |

## Conventions
- Scripts assume local gateway at `http://localhost:8080` unless overridden.
- Do not commit real admin credentials; use env vars.

## Outputs
- `summary.json` is written at repo root by `handleSummary()`.

## Commands
```bash
k6 run k6/sse-throughput-test.js
```
