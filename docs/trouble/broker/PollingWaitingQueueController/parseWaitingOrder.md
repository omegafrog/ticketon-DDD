# GET /api/v1/broker/polling/events/{id}/current Trouble

## Before

- 순번 조회는 단순 rank 표시가 아니라 상태별 응답 contract를 제공해야 한다.
- ENTRY token이 있으면 WAITING rank보다 그 상태를 우선해야 한다.
- poll-after-ms는 고정값이 아니라 queue 압력과 event 상태를 반영해야 한다.

## Decision Points

- entry token이 있으면 ENTRY 상태와 token을 반환한다.
- rank가 있으면 WAITING 상태와 1-based rank를 반환한다.
- rank가 없으면 NONE 상태를 반환하고 polling interval을 보수적으로 잡는다.

## Failure Modes

- ENTRY와 WAITING을 같은 응답으로 뭉개면 client가 다음 행동을 결정할 수 없다.
- TTL 갱신이 빠지면 user queue event key가 먼저 만료되어 ghost session처럼 보일 수 있다.
- queue가 커질수록 poll-after-ms를 늘리지 않으면 Redis read pressure가 급증한다.

## Why It Matters

- parseWaitingOrder는 UX와 backend load balancing을 동시에 맞춰야 한다.

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `afd3c9b` (2026-02-04): feat: Polling 대기열 시스템 구현 및 Redis 큐 구조 최적화



## Related Docs

- [Use Case](../../usecase/broker/PollingWaitingQueueController/parseWaitingOrder.md)
- [Flow](../../flow/broker/PollingWaitingQueueController/parseWaitingOrder.md)
- [Troubleshooting](../../troubleshooting/broker/PollingWaitingQueueController/parseWaitingOrder.md)
