# platform/message

Cross-service message/event contracts (DTOs) used for messaging and integration.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Message types | `platform/message/src/main/java/org/codenbug/message/` | Keep backwards compatible |
| Kafka notes | `docs/platform/message/kafka.md` | Operational notes |

## Naming / Organization
| Guideline | Notes |
|----------|------|
| Event naming | Prefer explicit domain verbs/nouns |
| Package stability | Avoid moving classes across packages |
| Consumer impact | Check all downstream modules |

## Conventions
- Treat message types as published contracts.
- Prefer additive changes (new fields) over breaking removals/renames.
- When behavior changes, consider versioning or introducing a new message type.

## Anti-Patterns
- Renaming/removing fields without checking consumers.
- Using message DTOs as internal domain entities.

## Commands
```bash
./gradlew :platform:message:test
```
