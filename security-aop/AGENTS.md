# Security AOP (Cross-Cutting)

`security-aop/` provides security annotations and aspects used across services.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Aspects/annotations | `security-aop/src/main/java/org/codenbug/securityaop/aop/` | `@AuthNeeded`, `@RoleRequired`, etc. |
| User context | `security-aop/src/main/java/org/codenbug/securityaop/aop/LoggedInUserContext.java` | Propagates user info |
| Config/secrets | `security-aop/src/main/resources/application-secret.yml` | Sensitive |
| Docs | `docs/security-aop/` | AOP notes |

## Usage Pattern
| Goal | Hint | Notes |
|------|------|------|
| Require login | `@AuthNeeded` | Applied on controllers/handlers |
| Require role | `@RoleRequired(...)` | Centralized authz check |
| Access user | `LoggedInUserContext` | Prefer request-scoped context |

## Conventions
- Keep this module generic and reusable; avoid service-specific dependencies.
- Aspects should fail closed; avoid silent auth bypass.

## Anti-Patterns
- Parsing tokens in every service controller instead of using this module.
- Swallowing auth exceptions in aspects/handlers.

## Commands
```bash
./gradlew :security-aop:test
```
