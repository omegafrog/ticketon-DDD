# POST /api/v1/payments/confirm Troubleshooting

## Current State

- 현재 verify command는 worker/test 기준으로 확인한다.
- confirm path는 PG 진입 전에 version guard를 통과해야 한다.

## Verification

- tests=1, failures=0, errors=0
- PG confirm 진입 수가 0인지 확인한다.
- finalization 진입 수가 0인지 확인한다.

## Quantitative Notes

- terminal states: `3` (`DONE`, `FAILED`, `REJECTED`)
- retries are bounded by scheduler and projection state, not controller calls

## Recent History

- [purchase-worker] `8a6fc92` (2026-04-02): docs: 문서 동기화
- [purchase-worker] `9fcb0cc` (2026-04-02): refactor(event): holder를 사용해서 purchase concurrency를 관리하지 않도록 수정
- [purchase-worker] `522f561` (2026-04-01): feat(purchase): EventSourcing 기반 결제 Confirm 플로우 + 스케줄러 기반 재시도 (#9)
- [controller] `55be56e` (2026-03-31): refactor: split command/query layers and harden MySQL replica bootstrap (#8)
- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [purchase-worker] `4c79957` (2026-03-31): refactor(purchase): replace logical hold with pessimistic DB row lock during PG confirm
- [controller] `7e32099` (2026-03-27): refactor(purchase): split cancel service and remove legacy payment flows (#6)
- [controller] `836c064` (2026-03-27): feat(purchase): use eventsourcing for payment confirm flow (#4)
- [controller] `ed148bf` (2026-02-05): feat: 결제 모듈 이벤트 소싱 전환 및 이벤트 점유(Hold) 로직 구현



## Related Docs

- [Use Case](../../usecase/purchase/PurchaseCommandController/confirmPayment.md)
- [Flow](../../flow/purchase/PurchaseCommandController/confirmPayment.md)
- [Trouble](../../trouble/purchase/PurchaseCommandController/confirmPayment.md)
