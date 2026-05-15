# Agent Commands and Verification

Use this file instead of repeating long command blocks in prompts.

## Environment

```bash
export JAVA_HOME=/home/jiwoo/.sdkman/candidates/java/current
export PATH="$JAVA_HOME/bin:$PATH"
```

Use `python3`. For Python dependencies, create/use repository-root `.venv/`.

## Gradle

```bash
./gradlew test --console=plain
./gradlew build --console=plain
./gradlew :purchase:test --console=plain
./gradlew :platform:gateway:bootRun
./gradlew :platform:eureka:bootRun
./gradlew :app:bootRun
./gradlew :auth:bootRun
./gradlew :broker:bootRun
./gradlew :dispatcher:bootRun
```

## Local Infra

```bash
docker-compose -f docker/docker-compose.yml ps
docker-compose -f docker/docker-compose.yml up -d
```

## Runtime Verification

When asked to verify that a server starts normally:
1. Check Docker infra with `docker-compose -f docker/docker-compose.yml ps`.
2. Start target service with `--debug-jvm` so JDWP opens on `5005`.
3. Attach with JDB MCP, or fallback to `jdb -attach 5005`.
4. Confirm readiness via HTTP status/health endpoint.
5. Stop service unless persistence was requested.

Keep logs in `/tmp`. Summarize key lines only. Do not print secrets.

## Output Budget Rules

Prefer these defaults:
- `max_output_tokens: 4000` for routine commands.
- `git status --porcelain=v1 -uno` instead of full status.
- `git diff --stat` before targeted `git diff -- <path>`.
- `rg -n "pattern" path -m 80` before reading file sections.
- `sed -n 'start,endp' file` for targeted reads.
- For tests, report failing test names and first relevant stack frame, not full XML/log output.

If command output is expected to be large, redirect logs to `/tmp` and inspect with `tail`, `rg`, or XML parsers.
