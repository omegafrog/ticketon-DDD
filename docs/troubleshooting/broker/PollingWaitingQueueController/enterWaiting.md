# GET /api/v1/broker/polling/events/{id}/waiting Troubleshooting

## Current State

- SSE는 사용하지 않고 polling queue state만 유지한다.
- entry token은 `5m` TTL로 저장된다.
- user queue event TTL은 `30s`로 갱신된다.

## Verification

- entry token이 이미 있으면 중복 entry가 차단되는지 본다.
- waiting zset과 waiting idx가 동시에 저장되는지 확인한다.
- 다른 event에 이미 속한 user가 hopping하지 못하는지 본다.

## Quantitative Notes

- entry token TTL: `5m`
- queue event TTL: `30s`
- poll-after-ms buckets: `1000/3000/5000/7000/8000/10000/30000`

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `afd3c9b` (2026-02-04): feat: Polling 대기열 시스템 구현 및 Redis 큐 구조 최적화



## Related Docs

- [Use Case](../../usecase/broker/PollingWaitingQueueController/enterWaiting.md)
- [Flow](../../flow/broker/PollingWaitingQueueController/enterWaiting.md)
- [Trouble](../../trouble/broker/PollingWaitingQueueController/enterWaiting.md)
