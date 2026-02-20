# Auth Service

Auth is the dedicated authentication/authorization service (OAuth/JWT) and provides login + token flows.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Entry point | `auth/src/main/java/org/codenbug/auth/AuthApplication.java` | Boot app |
| HTTP layer | `auth/src/main/java/org/codenbug/auth/ui/` | Controllers/requests/responses |
| App layer | `auth/src/main/java/org/codenbug/auth/app/` | Use-cases/services |
| Infra clients | `auth/src/main/java/org/codenbug/auth/infra/` | Redis/DB, inter-service clients |
| Config | `auth/src/main/resources/application*.yml` | Secrets in `application-secret.yml` |
| Docs | `docs/auth/` | API notes + OAuth refactors |

## Common Flows
| Flow | Hint | Notes |
|------|------|------|
| Login/OAuth | `docs/auth/login_refactoring_oauth.md` | Social login nuances |
| Validation calls | `auth/src/main/java/org/codenbug/auth/infra/` | Keep HTTP clients here |

## Conventions
- Token/JWT and OAuth credentials live in `application-secret.yml`; never log them.
- If calling other services, do it via explicit infra clients under `infra/`.

## Anti-Patterns
- Putting OAuth client secrets into non-secret configs or logs.
- Coupling auth domain logic to a single downstream service; keep boundaries clean.

## Commands
```bash
./gradlew :auth:bootRun
./gradlew :auth:test
```
