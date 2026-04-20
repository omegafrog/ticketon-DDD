# GET /api/v1/broker/polling/events/{id}/waiting Trouble

## Before

- polling 진입은 SSE 대신 Redis queue state를 먼저 만든다.
- entry token과 waiting zset이 함께 있어야 이후 parseWaitingOrder가 의미를 가진다.
- eventId별 중복 진입은 user queue event guard로 막아야 한다.

## Decision Points

- 로그인 userId 기준으로 entry token 존재 여부를 먼저 검사한다.
- user queue event key를 setIfAbsent로 잡아 event hopping을 방지한다.
- rank는 waiting zset과 waiting idx를 같이 써서 계산 가능한 상태로 만든다.

## Failure Modes

- entry token을 먼저 만들고 queue state를 늦게 만들면 중간 실패 시 복구가 어렵다.
- 순번 저장이 빠지면 parseWaitingOrder가 WAITING/ENTRY 상태를 재현하지 못한다.
- 다른 event에 이미 대기 중인 사용자를 풀어주지 않으면 queue hopping이 발생한다.

## Why It Matters

- enterWaiting은 polling 전체 흐름의 시작점이라, 작은 오염도 이후 조회와 탈출을 모두 망가뜨린다.

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `afd3c9b` (2026-02-04): feat: Polling 대기열 시스템 구현 및 Redis 큐 구조 최적화



## Related Docs

- [Use Case](../../usecase/broker/PollingWaitingQueueController/enterWaiting.md)
- [Flow](../../flow/broker/PollingWaitingQueueController/enterWaiting.md)
- [Troubleshooting](../../troubleshooting/broker/PollingWaitingQueueController/enterWaiting.md)
