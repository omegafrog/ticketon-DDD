# ENTRY_TOKEN TTL 미적용 문제

## 문제
- 토큰은 `ENTRY_TOKEN` 해시에 저장하지만 TTL은 `ENTRY_TOKEN:<userId>`에 설정하고 있었다.
- 실제 저장된 해시 키에는 TTL이 걸리지 않아 토큰이 만료되지 않는다.
- 결과적으로 토큰 누적 및 유효기간 관리가 틀어질 수 있다.

## 원인
- Redis는 해시의 필드 단위 TTL을 지원하지 않는다.
- 키 단위 만료만 가능하므로, 해시 필드에 TTL을 거는 방식은 동작하지 않는다.

## 해결
- 토큰을 유저별 키로 분리하여 저장한다.
  - 저장: `ENTRY_TOKEN:<userId>`
  - 만료: `SET ... EX` 또는 `set(key, value, ttl)` 방식
- 토큰 삭제 및 검증 로직을 유저별 키 조회/삭제로 변경한다.

## 변경 사항
- `broker/src/main/java/org/codenbug/broker/infra/EntryStreamMessageListener.java`
  - 토큰 저장을 `opsForValue().set(ENTRY_TOKEN:<userId>, token, TTL)`로 변경
- `broker/src/main/java/org/codenbug/broker/service/SseEmitterService.java`
  - 토큰 삭제를 유저 키 기준으로 변경
- `platform/common/src/main/java/org/codenbug/common/redis/EntryTokenValidator.java`
  - 토큰 검증을 유저 키 기준으로 변경
- `redislock/src/main/java/org/codenbug/redislock/RedisLockServiceImpl.java`
  - 토큰 삭제 로직을 유저 키 기준으로 변경

