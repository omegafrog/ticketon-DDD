# Seat Service

Seat owns seat layouts and availability, and serves internal endpoints for purchase/event flows.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| UI controllers | `seat/src/main/java/org/codenbug/seat/ui/` | External/internal endpoints |
| Domain | `seat/src/main/java/org/codenbug/seat/domain/` | Seat/SeatLayout |
| App layer | `seat/src/main/java/org/codenbug/seat/app/` | Use-cases |
| Infra adapters | `seat/src/main/java/org/codenbug/seat/infra/` | Persistence + service clients |
| Config | `seat/src/main/resources/application.yml` | Port 9005 |
| Docs | `docs/seat/README.md` | Module notes |

## Common Entry Points
| Area | Location | Notes |
|------|----------|------|
| Internal API | `seat/src/main/java/org/codenbug/seat/ui/SeatInternalController.java` | Cross-service use |
| Service clients | `seat/src/main/java/org/codenbug/seat/infra/` | Event-service calls |

## Conventions
- Service-to-service calls belong in `infra/` (e.g., Event service client).

## Anti-Patterns
- Leaking external API DTOs into the domain layer.
- Doing remote calls from domain objects.

## Commands
```bash
./gradlew :seat:test
```
