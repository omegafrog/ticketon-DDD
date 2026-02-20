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