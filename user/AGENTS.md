# User Service

User owns user profile, registration-related flows, and user-facing validation endpoints.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| UI controllers | `user/src/main/java/org/codenbug/user/ui/` | User APIs + validation |
| Domain | `user/src/main/java/org/codenbug/user/domain/` | User aggregate, IDs |
| App layer | `user/src/main/java/org/codenbug/user/app/` | Use-cases |
| Infra adapters | `user/src/main/java/org/codenbug/user/infra/` | Persistence + consumers |
| Config | `user/src/main/resources/application.yml` | Port 9004 |
| Docs | `docs/user/README.md` | Module notes |
| Transactions | `docs/user/transaction.md` | Listener/transaction patterns |

## Key Building Blocks
| Topic | Location | Notes |
|------|----------|------|
| Validation API | `user/src/main/java/org/codenbug/user/ui/UserValidationController.java` | Used by auth flows |
| Consumers | `user/src/main/java/org/codenbug/user/infra/consumer/` | Messaging integration |

## Conventions
- Messaging consumers live under `infra/consumer/`.

## Anti-Patterns
- Mixing consumer side effects into domain entities.
- Making validation endpoints depend on UI-layer state.

## Commands
```bash
./gradlew :user:test
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