# Seat Service

Seat owns seat layouts and availability, and serves internal endpoints for purchase/event flows.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| UI controllers | `seat/src/main/java/org/codenbug/seat/ui/` | External/internal endpoints |
| Domain | `seat/src/main/java/org/codenbug/seat/domain/` | Seat/SeatLayout |
| App layer | `seat/src/main/java/org/codenbug/seat/app/` | Use-cases |
| Infra adapters | `seat/src/main/java/org/codenbug/seat/infra/` | Persistence + service clients |
| Config | `seat/src/main/resources/application.yml` | Port 9005 |
| Docs | `docs/seat/README.md` | Module notes |

## Common Entry Points
| Area | Location | Notes |
|------|----------|------|
| Internal API | `seat/src/main/java/org/codenbug/seat/ui/SeatInternalController.java` | Cross-service use |
| Service clients | `seat/src/main/java/org/codenbug/seat/infra/` | Event-service calls |

## Conventions
- Service-to-service calls belong in `infra/` (e.g., Event service client).

## Anti-Patterns
- Leaking external API DTOs into the domain layer.
- Doing remote calls from domain objects.

## Commands
```bash
./gradlew :seat:test
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