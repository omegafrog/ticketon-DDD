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
