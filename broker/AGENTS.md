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
