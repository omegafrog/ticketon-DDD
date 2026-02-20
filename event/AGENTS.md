# Event Service

Event owns event creation/updates, event queries, and related projections.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| UI controllers | `event/src/main/java/org/codenbug/event/ui/` | Command/query/internal endpoints |
| Domain | `event/src/main/java/org/codenbug/event/domain/` | Core aggregates/entities |
| App layer | `event/src/main/java/org/codenbug/event/application/` | Use-cases/services |
| Query/projections | `event/src/main/java/org/codenbug/event/query/` | Read models, projections |
| Infra | `event/src/main/java/org/codenbug/event/infra/` | Persistence implementations |
| Tests | `event/src/test/java/` | Includes integration-style flow tests |
| Config | `event/src/main/resources/application*.yml` | Dev/prod variants |
| Docs | `docs/event/README.md` | Module notes |

## Common Entry Points
| Area | Location | Notes |
|------|----------|------|
| Queries | `event/src/main/java/org/codenbug/event/ui/EventQueryController.java` | Read APIs |
| Commands | `event/src/main/java/org/codenbug/event/ui/EventCommandController.java` | Write APIs |
| Internal | `event/src/main/java/org/codenbug/event/ui/EventInternalController.java` | Cross-service endpoints |
| Uploads | `event/src/main/java/org/codenbug/event/ui/FileUploadController.java` | Async upload behavior |

## Conventions
- Query code tends to live under `query/` (projections, read repositories).
- Keep controller DTOs in `ui/` and persistence adapters in `infra/`.

## Anti-Patterns
- Mixing persistence-specific details into controller DTOs.
- Putting query projection logic into domain entities.

## Commands
```bash
./gradlew :event:test
```
