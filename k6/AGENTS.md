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

## Runtime Verification Policy (Agent)
- When validating "server starts normally" or runtime behavior, prefer JDB MCP verification over log-only checks.
- Default flow:
  1) Start target service in debug mode (`--debug-jvm`) so JDWP opens on port `5005`.
  2) Attach via JDB MCP (or `jdb -attach 5005` as fallback) and verify thread/session visibility.
  3) Confirm service readiness via HTTP status or health endpoint.
  4) Stop the service unless persistence is explicitly requested.
- Keep verification evidence in temporary logs (for example under `/tmp`) and summarize key lines in the response.
- Do not expose secrets while collecting runtime evidence.

## Pull Request Writing Policy (Agent)
- When creating a PR, write a detailed body by default.
- Include `## Summary`, `## Changes`, `## Verification`, and `## Risks and Rollback` sections.
- Explain scope boundaries (what is included and excluded) so reviewers can quickly assess impact.
- Link follow-up tasks or TODOs when work is intentionally deferred.
