# Broker Service (SSE / Waiting Queue)

Broker manages SSE connections and the waiting queue (Redis-backed) for high-traffic entry.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Entry point | `broker/src/main/java/org/codenbug/broker/BrokerApplication.java` | Boot app |
| HTTP/SSE controllers | `broker/src/main/java/org/codenbug/broker/ui/` | SSE + polling APIs |
| App/services | `broker/src/main/java/org/codenbug/broker/service/` | SSE emitter orchestration |
| Redis + infra | `broker/src/main/java/org/codenbug/broker/infra/` | Queue repos, schedulers, listeners |
| Config | `broker/src/main/resources/application*.yml` | Includes `application-polling.yml` |
| Docs | `docs/broker/` | Queue + SSE notes |
| Troubleshooting | `docs/troubleshooting/` | TTL, dispatch responsibility, queue counts |

## Key Concepts
| Topic | Hint | Notes |
|------|------|------|
| SSE lifecycle | `broker/src/main/java/org/codenbug/broker/service/SseEmitterService.java` | Clean up on disconnect |
| Redis queue state | `broker/src/main/java/org/codenbug/broker/infra/WaitingQueueRedisRepository.java` | Atomicity + TTL |
| Polling mode | `broker/src/main/resources/application-polling.yml` | Separate profile |

## Conventions
- Queue state is Redis-centric; prefer atomic operations and careful TTL semantics.
- Polling mode is configured via `application-polling.yml`.

## Anti-Patterns
- Leaving SSE emitters/queues orphaned on client abort.
- Making multi-step Redis updates without atomic guards.

## Commands
```bash
./gradlew :broker:bootRun
./gradlew :broker:test
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