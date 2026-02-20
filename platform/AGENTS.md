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


## Runtime Verification Policy (Agent)
- When validating "server starts normally" or runtime behavior, prefer JDB MCP verification over log-only checks.
- Default flow:
  1) Start target service in debug mode (`--debug-jvm`) so JDWP opens on port `5005`.
  2) Attach via JDB MCP (or `jdb -attach 5005` as fallback) and verify thread/session visibility.
  3) Confirm service readiness via HTTP status or health endpoint.
  4) Stop the service unless persistence is explicitly requested.
- Keep verification evidence in temporary logs (for example under `/tmp`) and summarize key lines in the response.
- Do not expose secrets while collecting runtime evidence.

## Pull Request Writing Policy (Agent)
- When creating a PR, write a detailed body by default.
- Include `## Summary`, `## Changes`, `## Verification`, and `## Risks and Rollback` sections.
- Explain scope boundaries (what is included and excluded) so reviewers can quickly assess impact.
- Link follow-up tasks or TODOs when work is intentionally deferred.