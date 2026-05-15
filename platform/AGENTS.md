# Platform Modules

Purpose: shared infrastructure modules.

Structure:
- `platform/common`: shared utilities and exceptions
- `platform/message`: cross-service message/event DTOs
- `platform/gateway`: Spring Cloud Gateway
- `platform/eureka`: Eureka discovery server

Rules:
- No service-specific business/domain logic in platform modules.
- Keep shared types small and backward-compatible.
- Never print or commit `application-secret.yml`.

Commands:
- `./gradlew :platform:common:test`
- `./gradlew :platform:message:test`
- `./gradlew :platform:gateway:bootRun`
- `./gradlew :platform:eureka:bootRun`
