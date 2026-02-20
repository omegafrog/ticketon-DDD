# Event Service

Event owns event creation/updates, event queries, and related projections.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| UI controllers | `event/src/main/java/org/codenbug/event/ui/` | Command/query/internal endpoints |
| Domain | `event/src/main/java/org/codenbug/event/domain/` | Core aggregates/entities |
| App layer | `event/src/main/java/org/codenbug/event/application/` | Use-cases/services |
| Query/projections | `event/src/main/java/org/codenbug/event/query/` | Read models, projections |
| Infra | `event/src/main/java/org/codenbug/event/infra/` | Persistence implementations |
| Tests | `event/src/test/java/` | Includes integration-style flow tests |
| Config | `event/src/main/resources/application*.yml` | Dev/prod variants |
| Docs | `docs/event/README.md` | Module notes |

## Common Entry Points
| Area | Location | Notes |
|------|----------|------|
| Queries | `event/src/main/java/org/codenbug/event/ui/EventQueryController.java` | Read APIs |
| Commands | `event/src/main/java/org/codenbug/event/ui/EventCommandController.java` | Write APIs |
| Internal | `event/src/main/java/org/codenbug/event/ui/EventInternalController.java` | Cross-service endpoints |
| Uploads | `event/src/main/java/org/codenbug/event/ui/FileUploadController.java` | Async upload behavior |

## Conventions
- Query code tends to live under `query/` (projections, read repositories).
- Keep controller DTOs in `ui/` and persistence adapters in `infra/`.

## Anti-Patterns
- Mixing persistence-specific details into controller DTOs.
- Putting query projection logic into domain entities.

## Commands
```bash
./gradlew :event:test
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