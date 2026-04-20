# POST /api/v1/payments/{paymentKey}/cancel Troubleshooting

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

- [controller] `55be56e` (2026-03-31): refactor: split command/query layers and harden MySQL replica bootstrap (#8)
- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `7e32099` (2026-03-27): refactor(purchase): split cancel service and remove legacy payment flows (#6)
- [controller] `836c064` (2026-03-27): feat(purchase): use eventsourcing for payment confirm flow (#4)
- [controller] `ed148bf` (2026-02-05): feat: 결제 모듈 이벤트 소싱 전환 및 이벤트 점유(Hold) 로직 구현



## Related Docs

- [Use Case](../../usecase/purchase/PurchaseCommandController/cancelPayment.md)
- [Flow](../../flow/purchase/PurchaseCommandController/cancelPayment.md)
- [Trouble](../../trouble/purchase/PurchaseCommandController/cancelPayment.md)
