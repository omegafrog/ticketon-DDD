# GET /api/v1/broker/polling/events/{id}/current Troubleshooting

## Current State

- ENTRY token이 있으면 ENTRY 상태가 우선한다.
- WAITING 상태는 zset rank + 1로 표시한다.
- NONE 상태는 queue 밖의 사용자다.

## Verification

- rank 없는 사용자가 token 없이 WAITING으로 오인되지 않는지 본다.
- queue pressure에 따라 poll interval이 실제로 늘어나는지 확인한다.
- event 상태가 OPEN이 아닐 때 30s 반환이 동작하는지 본다.

## Quantitative Notes

- rank thresholds: `10`, `100`
- poll intervals: `1000`, `3000`, `5000`, `7000`, `8000`, `10000`, `30000`

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `afd3c9b` (2026-02-04): feat: Polling 대기열 시스템 구현 및 Redis 큐 구조 최적화



## Related Docs

- [Use Case](../../usecase/broker/PollingWaitingQueueController/parseWaitingOrder.md)
- [Flow](../../flow/broker/PollingWaitingQueueController/parseWaitingOrder.md)
- [Trouble](../../trouble/broker/PollingWaitingQueueController/parseWaitingOrder.md)
