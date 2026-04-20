# DELETE /api/v1/broker/polling/events/{id}/waiting Trouble

## Before

- disconnect는 entry token과 waiting record를 동시에 치워야 한다.
- slot 복구가 늦으면 다음 사용자 승급이 막힌다.
- polling mode에서는 SSE close가 없으므로 cleanup 책임이 service로 완전히 내려간다.

## Decision Points

- entry token 삭제 후 waiting zset 제거를 수행한다.
- entry slot을 `+1` 되돌려 capacity를 회수한다.
- user queue event key와 last seen 관련 상태를 함께 정리한다.

## Failure Modes

- token만 지우고 zset을 남기면 순번 조회가 dangling state를 반환한다.
- slot 복구가 빠지면 queue throughput이 떨어진다.
- 대기열 탈출이 부분적으로만 되면 재진입/재조회 모두 오염된다.

## Why It Matters

- disconnectWaiting은 polling queue의 정리점이자 capacity release 지점이다.

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `afd3c9b` (2026-02-04): feat: Polling 대기열 시스템 구현 및 Redis 큐 구조 최적화



## Related Docs

- [Use Case](../../usecase/broker/PollingWaitingQueueController/disconnectWaiting.md)
- [Flow](../../flow/broker/PollingWaitingQueueController/disconnectWaiting.md)
- [Troubleshooting](../../troubleshooting/broker/PollingWaitingQueueController/disconnectWaiting.md)
