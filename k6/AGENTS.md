# k6 Load Tests

Purpose: load-test scripts for gateway, broker, queue, and Redis-backed flows.

Look here:
- SSE throughput: `k6/sse-throughput-test.js`
- Queue comparison: `k6/queue-impact-comparison.js`
- Result comparison: `k6/compare_queue_results.py`

Common env:
- `BASE_URL`, default `http://localhost:8080`
- `BROKER_BASE_URL`, default `BASE_URL`
- `REDIS_URL`, default `redis://127.0.0.1:6379`
- `EVENT_IDS`, comma-separated IDs
- `ADMIN_EMAIL` / `ADMIN_PASSWORD`, via env only

Rules:
- Do not commit real credentials.
- Keep large generated summaries out of prompts; parse key metrics.

Command:
- `k6 run k6/sse-throughput-test.js`
