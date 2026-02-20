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