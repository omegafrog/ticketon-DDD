# App (Orchestrator)

The `app/` module is the orchestration Spring Boot application. It should only import/wire configuration from other modules.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Entry point | `app/src/main/java/org/codenbug/app/AppApplication.java` | Main bootstrap |
| Wiring/config | `app/src/main/java/org/codenbug/app/config/` | DataSources, Jackson, OpenAPI, static resources |
| App config | `app/src/main/resources/application*.yml` | `application-secret.yml` is sensitive |

## Common Wiring Spots
| Topic | Location | Notes |
|------|----------|------|
| Data sources | `app/src/main/java/org/codenbug/app/config/DatabaseConfig.java` | Primary + read-only |
| Querydsl | `app/src/main/java/org/codenbug/app/config/QueryFactoryConfig.java` | Query factory wiring |
| JSON | `app/src/main/java/org/codenbug/app/config/JacksonConfig.java` | ObjectMapper config |
| OpenAPI | `app/src/main/java/org/codenbug/app/config/AppOpenApiConfig.java` | API docs wiring |
| Static | `app/src/main/java/org/codenbug/app/config/StaticResourceConfig.java` | Static assets |

## Conventions
- Keep `app/` free of business logic: no domain services, repositories, controllers, or infra adapters.
- If you need new integration wiring, add it as configuration in `org.codenbug.app.config`.

## Anti-Patterns
- Adding `@RestController` or persistence code in `app/`.
- Adding service-specific domain models here; belong in the owning module.
- Adding per-service configuration that should live in the service module.

## Commands
```bash
./gradlew :app:bootRun
./gradlew :app:test
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