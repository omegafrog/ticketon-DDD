# platform/eureka

Eureka server for service discovery.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Entry point | `platform/eureka/src/main/java/org/codenbug/eureka/EurekaApplication.java` | Port 8761 |
| Config | `platform/eureka/src/main/resources/application.yml` | Server settings |

## Operational Notes
| Topic | Notes |
|------|------|
| Dashboard | Browse `http://localhost:8761` when running locally |
| Client config | Services typically set `eureka.client.serviceUrl.defaultZone` |
| Local dev | Start Eureka before services that register |

## Conventions
- Keep this module minimal; most changes are config-level.
- Client services should register against Eureka; avoid hardcoding internal service URLs.

## Anti-Patterns
- Adding business logic or cross-service coupling into Eureka.
- Treating Eureka as a config store; keep it for discovery only.
- Hardcoding service URLs when `lb://` discovery is expected.

## Commands
```bash
./gradlew :platform:eureka:bootRun
```
