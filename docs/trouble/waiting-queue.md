# Waiting Queue Trouble

## Before

- 중복 진입 기록이 없으면 같은 userId가 여러 인스턴스에서 동시에 진입할 수 있었다.
- `ENTRY_TOKEN`을 hash 필드처럼 다루면 TTL이 실제 키에 걸리지 않았다.
- `EntryStreamMessageListener`가 토큰 정리, 상태 변경, ack를 한 파일에서 처리해 책임이 과했다.
- Lua 스크립트가 중간 오류에서 `error()`로 끝나면, 이미 반영된 승격과 반환값이 어긋날 수 있었다.
- `948bb7f`에서 ack와 message handling이 개선되기 전에는 `sseConnection == null` 경로가 NPE와 pending 누적으로 이어질 수 있었다.
- `0c89f45` 이전에는 `WAITING_USER_IDS` 기반 원자 중복 방지가 없었다.
- `b5ecf96` 이전에는 `ENTRY_TOKEN`이 hash 저장 방식이라 TTL을 실질적으로 적용할 수 없었다.
- `6215885` 이전에는 Lua 스크립트가 중간 실패 시 전체 종료 쪽에 가까웠다.

## Why It Was a Problem

- scale-out 상태에서 duplicate enter는 재현이 쉽다.
- TTL이 없는 토큰은 쌓이고, 유효성 판단도 흐려진다.
- 중간 실패가 전체 실패로 보이면 `ENTRY_QUEUE_SLOTS`와 실제 승격 수가 달라진다.

## What Needed To Change

- 중복 진입은 Redis 원자 연산으로 막아야 한다.
- 토큰은 hash가 아니라 per-user string key로 저장해야 한다.
- ack 책임과 도메인 처리를 분리해야 한다.
- Lua는 실패 아이템만 건너뛰고 실제 성공 수를 반환해야 한다.

## Related Docs

- [Use Case](../usecase/waiting-queue.md)
- [Flow](../flow/waiting-queue.md)
- [Troubleshooting](../troubleshooting/waiting-queue.md)
- Legacy: `docs/troubleshooting/waiting-queue-in-user-record.md`
- Legacy: `docs/troubleshooting/entry-token-ttl.md`
- Legacy: `docs/troubleshooting/promotion-partial-failure.md`
