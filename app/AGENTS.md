# App Module

Purpose: orchestration Spring Boot app. It imports and wires config from other modules only.

Look here:
- Entry point: `app/src/main/java/org/codenbug/app/AppApplication.java`
- Config: `app/src/main/java/org/codenbug/app/config/`
- Resources: `app/src/main/resources/application*.yml`

Rules:
- No business logic, controllers, repositories, domain models, or service-specific infra here.
- Add integration wiring as config under `org.codenbug.app.config`.
- Never print `application-secret.yml`.

Commands:
- `./gradlew :app:test`
- `./gradlew :app:bootRun`

Shared context: `../docs/agent/context.md`, `../docs/agent/commands.md`.
