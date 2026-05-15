# Auth Module

Purpose: authentication/authorization service for login, OAuth, JWT, and token flows.

Look here:
- Entry point: `auth/src/main/java/org/codenbug/auth/AuthApplication.java`
- HTTP layer: `auth/src/main/java/org/codenbug/auth/ui/`
- App layer: `auth/src/main/java/org/codenbug/auth/app/`
- Infra clients: `auth/src/main/java/org/codenbug/auth/infra/`
- Config: `auth/src/main/resources/application*.yml`

Rules:
- Keep OAuth/JWT secrets only in secret config; never log them.
- Put downstream service calls in explicit infra clients.

Commands:
- `./gradlew :auth:test`
- `./gradlew :auth:bootRun`
