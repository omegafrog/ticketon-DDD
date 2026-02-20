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
