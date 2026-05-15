# Dispatcher Module

Purpose: worker that promotes users from waiting queue to active entry.

Look here:
- Entry point: `dispatcher/src/main/java/org/codenbug/messagedispatcher/MessageDispatcherApplication.java`
- Redis logic: `dispatcher/src/main/java/org/codenbug/messagedispatcher/redis/`
- Threading: `dispatcher/src/main/java/org/codenbug/messagedispatcher/thread/`
- Config: `dispatcher/src/main/resources/application*.yml`
- CI workflow: `.github/workflows/disptach-ci.yml`

Rules:
- Promotion order and concurrency are sensitive; keep edits small and test-backed.
- Do not increase concurrency without validating Redis/DB impact.

Commands:
- `./gradlew :dispatcher:test`
- `./gradlew :dispatcher:bootRun`
