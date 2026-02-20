# User Service

User owns user profile, registration-related flows, and user-facing validation endpoints.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| UI controllers | `user/src/main/java/org/codenbug/user/ui/` | User APIs + validation |
| Domain | `user/src/main/java/org/codenbug/user/domain/` | User aggregate, IDs |
| App layer | `user/src/main/java/org/codenbug/user/app/` | Use-cases |
| Infra adapters | `user/src/main/java/org/codenbug/user/infra/` | Persistence + consumers |
| Config | `user/src/main/resources/application.yml` | Port 9004 |
| Docs | `docs/user/README.md` | Module notes |
| Transactions | `docs/user/transaction.md` | Listener/transaction patterns |

## Key Building Blocks
| Topic | Location | Notes |
|------|----------|------|
| Validation API | `user/src/main/java/org/codenbug/user/ui/UserValidationController.java` | Used by auth flows |
| Consumers | `user/src/main/java/org/codenbug/user/infra/consumer/` | Messaging integration |

## Conventions
- Messaging consumers live under `infra/consumer/`.

## Anti-Patterns
- Mixing consumer side effects into domain entities.
- Making validation endpoints depend on UI-layer state.

## Commands
```bash
./gradlew :user:test
```
