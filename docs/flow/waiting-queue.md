# Waiting Queue Flow

## Flow Summary

대기열은 broker, dispatcher, Redis, SSE를 묶은 분산 흐름이다.
진입은 broker가 담당하고, 승격은 dispatcher가 담당하며, 최종 통지는 broker가 다시 받는다.

## Main Steps

1. 사용자가 broker의 SSE 엔드포인트로 진입한다.
2. broker가 `WAITING_USER_IDS`로 중복 진입을 막고, `WAITING_QUEUE_IDX`와 `WAITING_QUEUE_INDEX_RECORD`를 갱신한다.
3. broker가 SSE 연결과 큐 순번을 유지한다.
4. dispatcher가 1초 주기로 `WAITING:*`을 스캔해 승격 가능 대상을 찾는다.
5. Lua 스크립트가 `ENTRY_QUEUE_SLOTS` 기준으로 실제 승격 수를 계산하고 `ENTRY` stream에 메시지를 적재한다.
6. broker가 `DISPATCH:{instanceId}`를 받아 `EntryDispatchService`로 넘기고 SSE로 승격 통지를 보낸다.

## Redis Semantics

- `WAITING_USER_IDS:{eventId}`: 중복 진입 방지용 원자 기록
- `ENTRY_QUEUE_SLOTS`: 승격 가능한 slot 수
- `ENTRY_TOKEN:<userId>`: 5분 TTL 토큰

## Related Docs

- [Use Case](../usecase/waiting-queue.md)
- [Trouble](../trouble/waiting-queue.md)
- [Troubleshooting](../troubleshooting/waiting-queue.md)
- Legacy: `docs/broker/Waiting_queue.md`
- Legacy: `docs/dispatcher/waitingqueue.md`
