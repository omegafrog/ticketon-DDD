# GET /api/v1/refunds/my Troubleshooting

## Current State

- confirm/retry/finalization이 서로 다른 계층에서 수행된다.
- worker가 PG 호출 전에 version guard를 먼저 통과해야 한다.

## Verification

- terminal states에서 재시도가 멈추는지 본다.
- hold/lock/holder 변화가 외부 I/O와 분리되어 있는지 확인한다.

## Quantitative Notes

- terminal states: `3`
- worker-triggered PG calls should be `0` on stale version

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `a49d73c` (2025-09-10): refactor: 서비스 계층에서 독립된 redislock 모듈로 import 변경
- [controller] `239e44b` (2025-08-04): feat: notification 모듈 추가



## Related Docs

- [Use Case](../../usecase/purchase/RefundController/getMyRefunds.md)
- [Flow](../../flow/purchase/RefundController/getMyRefunds.md)
- [Trouble](../../trouble/purchase/RefundController/getMyRefunds.md)
