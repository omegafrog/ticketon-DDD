# Platform Modules

The `platform/` tree contains shared infrastructure modules used by multiple services.

## Structure
```
platform/
├── common/    # Shared utilities (exceptions, redis helpers, etc.)
├── message/   # Cross-service message/event DTOs
├── gateway/   # Spring Cloud Gateway entrypoint + routing/whitelist
└── eureka/    # Eureka server (service discovery)
```

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Change gateway routes | `platform/gateway/src/main/resources/application-gateway-routes-prod.yml` | Prod routing via `lb://...` |
| Change gateway whitelist | `platform/gateway/src/main/resources/application-whitelist-*.yml` | Local vs prod |
| Gateway boot | `platform/gateway/src/main/java/org/codenbug/gateway/GatewayApplication.java` | Port 8080 |
| Eureka boot | `platform/eureka/src/main/java/org/codenbug/eureka/EurekaApplication.java` | Port 8761 |
| Shared message contracts | `platform/message/src/main/java/org/codenbug/message/` | Keep stable, backwards compatible |
| Shared utilities | `platform/common/src/main/java/` | Prefer small, reusable pieces |

## Conventions
- Platform modules should not contain business/domain logic for a single service.
- Prefer minimal, stable interfaces/types; changes tend to ripple across services.
- Config secrets: `application-secret.yml` exists in some platform modules; never print/commit secrets.

## Commands
```bash
./gradlew :platform:gateway:bootRun
./gradlew :platform:eureka:bootRun
./gradlew :platform:common:test
./gradlew :platform:message:test
```
