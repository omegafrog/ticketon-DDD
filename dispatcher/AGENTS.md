# Dispatcher (Queue Promotion Worker)

Dispatcher is a worker that promotes users from waiting to active entry and coordinates dispatch logic.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Entry point | `dispatcher/src/main/java/org/codenbug/messagedispatcher/MessageDispatcherApplication.java` | Boot app |
| Redis config/logic | `dispatcher/src/main/java/org/codenbug/messagedispatcher/redis/` | Queue/redis operations |
| Threading | `dispatcher/src/main/java/org/codenbug/messagedispatcher/thread/` | Concurrency behavior |
| Config | `dispatcher/src/main/resources/application*.yml` | Dev/prod/secret variants |
| Docker | `dispatcher/Dockerfile.runtime` | CI uses runtime image |
| Docs | `docs/dispatcher/` | Promotion + queue notes |
| Ops | `docs/polling-queue/` | Verification commands |

## CI Notes
| Topic | Location | Notes |
|------|----------|------|
| Workflow | `.github/workflows/disptach-ci.yml` | Name is misspelled (`disptach`) |
| Packaging | `.github/workflows/disptach-ci.yml` | Builds `bootJar` then Docker runtime image |

## Conventions
- Dispatcher changes are highly concurrency-sensitive; prefer small, test-backed edits.

## Anti-Patterns
- Changing promotion ordering without a clear invariant and regression test.
- Increasing concurrency without validating Redis/DB impact.

## Commands
```bash
./gradlew :dispatcher:bootRun
./gradlew :dispatcher:test
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