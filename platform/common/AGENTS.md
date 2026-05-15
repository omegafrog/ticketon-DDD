# Platform Common

Purpose: shared utilities, exceptions, config helpers, and lightweight DTO/value objects.

Look here:
- Java sources: `platform/common/src/main/java/`
- Sensitive config: `platform/common/src/main/resources/application-secret.yml`

Rules:
- No service-specific business rules or dependencies on service modules.
- Prefer additive API changes; this module ripples across services.
- Never print or commit secret config.

Command:
- `./gradlew :platform:common:test`
