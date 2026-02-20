# platform/gateway

Spring Cloud Gateway. All client traffic enters through this service.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Entry point | `platform/gateway/src/main/java/org/codenbug/gateway/GatewayApplication.java` | Port 8080 |
| Filters | `platform/gateway/src/main/java/org/codenbug/gateway/filter/` | AuthZ, headers, etc. |
| Base config | `platform/gateway/src/main/resources/application.yml` | Uses `spring.config.import` |
| Prod routes | `platform/gateway/src/main/resources/application-gateway-routes-prod.yml` | `lb://auth`, `lb://broker`, `lb://app` |
| Whitelist | `platform/gateway/src/main/resources/application-whitelist-*.yml` | Local vs prod |

## Route / Filter Model
| Topic | Notes |
|------|------|
| Default filter | Applied broadly; be explicit about bypass |
| Docs routes | OpenAPI routes are explicitly mapped |
| Service URIs | Production uses `lb://` via Eureka |

## Conventions
- Route changes must consider both prod routes and whitelist enforcement.
- Default filters apply broadly; be explicit when bypassing auth.
- Keep routing config declarative in YAML where possible.

## Anti-Patterns
- Adding ad-hoc bypasses without updating whitelist and tests.
- Hardcoding host/port routes in prod when service discovery is expected.

## Commands
```bash
./gradlew :platform:gateway:bootRun
./gradlew :platform:gateway:test
```
