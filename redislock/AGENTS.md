# RedisLock Module

Purpose: Redis/Redisson locking utilities shared by services.

Look here:
- API: `redislock/src/main/java/org/codenbug/redislock/RedisLockService.java`
- Implementation: `redislock/src/main/java/org/codenbug/redislock/RedisLockServiceImpl.java`
- Redisson config: `redislock/src/main/java/org/codenbug/redislock/RedissonConfig.java`
- Config: `redislock/src/main/resources/application.properties`

Rules:
- Use per-resource lock keys.
- Always set wait and lease bounds.
- Fail fast rather than risking deadlock.

Command:
- `./gradlew :redislock:test`
