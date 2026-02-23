# RedisLock Utilities

`redislock/` provides Redis-based locking utilities used by other services.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Lock API | `redislock/src/main/java/org/codenbug/redislock/RedisLockService.java` | Interface |
| Implementation | `redislock/src/main/java/org/codenbug/redislock/RedisLockServiceImpl.java` | Main implementation |
| Redisson config | `redislock/src/main/java/org/codenbug/redislock/RedissonConfig.java` | Client wiring |
| Config | `redislock/src/main/resources/application.properties` | Redis endpoints |

## Usage Notes
| Topic | Notes |
|------|------|
| Lock scope | Prefer per-aggregate/per-resource keys |
| Timeouts | Always set wait/lease bounds |
| Failure mode | Fail fast rather than deadlock |

## Conventions
- Prefer short-lived locks with explicit timeouts.
- Keep lock key naming consistent across services.

## Anti-Patterns
- Locks without lease/timeout.
- Lock keys derived from mutable or non-unique identifiers.

## Commands
```bash
./gradlew :redislock:test
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