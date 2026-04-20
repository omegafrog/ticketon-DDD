# DELETE /api/v1/broker/polling/events/{id}/waiting Troubleshooting

## Current State

- disconnect는 entry token, waiting record, slot을 동시에 손본다.
- SSE cleanup이 아니라 Redis cleanup이 핵심이다.

## Verification

- entry slot이 `+1` 회복되는지 확인한다.
- waiting zset과 token이 모두 제거되는지 확인한다.

## Quantitative Notes

- slot restore delta: `+1`
- cleanup targets: `3+` (`ENTRY_TOKEN`, waiting zset, queue event key)

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `afd3c9b` (2026-02-04): feat: Polling 대기열 시스템 구현 및 Redis 큐 구조 최적화



## Related Docs

- [Use Case](../../usecase/broker/PollingWaitingQueueController/disconnectWaiting.md)
- [Flow](../../flow/broker/PollingWaitingQueueController/disconnectWaiting.md)
- [Trouble](../../trouble/broker/PollingWaitingQueueController/disconnectWaiting.md)
