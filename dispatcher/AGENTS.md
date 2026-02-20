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
