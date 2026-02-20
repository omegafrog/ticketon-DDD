# platform/common

Shared utilities used across services (exceptions, shared config helpers, common DTOs).

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Shared utilities | `platform/common/src/main/java/` | Keep small and stable |
| Secrets | `platform/common/src/main/resources/application-secret.yml` | Sensitive; often shared JWT/etc |

## Typical Contents
| Area | Notes |
|------|------|
| Exceptions | Shared exception types used across services |
| Redis helpers | Common Redis client/config patterns |
| DTO helpers | Lightweight shared DTO/value objects |

## Conventions
- Avoid service-specific logic; this module should not depend on business modules.
- Prefer additive changes; breaking API changes ripple across services.
- Keep `application-secret.yml` out of logs and commits.

## Anti-Patterns
- Adding business rules here because it is convenient.
- Adding dependencies from `platform/common` to a specific service module.

## Commands
```bash
./gradlew :platform:common:test
```
