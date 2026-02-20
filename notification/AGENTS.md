# Notification Service

Notification consumes domain events and provides user-facing notification APIs and emitters.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| UI/API | `notification/src/main/java/org/codenbug/notification/ui/` | Projections + endpoints |
| Application service | `notification/src/main/java/org/codenbug/notification/application/` | Use-cases |
| Messaging listeners | `notification/src/main/java/org/codenbug/notification/infrastructure/` | Event listeners + persistence |
| Domain | `notification/src/main/java/org/codenbug/notification/domain/` | Notification entities/value objects |
| Config | `notification/src/main/resources/application.yml` | RabbitMQ etc. |
| Docs | `docs/notification/README.md` | Module notes |

## Key Building Blocks
| Topic | Location | Notes |
|------|----------|------|
| Emitter | `notification/src/main/java/org/codenbug/notification/service/NotificationEmitterService.java` | Push notifications |
| Listeners | `notification/src/main/java/org/codenbug/notification/infrastructure/` | Event-driven updates |
| Views | `notification/src/main/java/org/codenbug/notification/ui/` | Projections and repositories |

## Conventions
- Keep event-handling adapters in `infrastructure/` and UI projections under `ui/`.

## Anti-Patterns
- Doing side effects directly in projection/read repositories.
- Making event listeners non-idempotent.

## Commands
```bash
./gradlew :notification:test
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