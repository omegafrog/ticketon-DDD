# GET /api/v1/payments/confirm/{purchaseId}/status Troubleshooting

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



## Related Docs

- [Use Case](../../usecase/purchase/PurchaseConfirmQueryController/getConfirmStatus.md)
- [Flow](../../flow/purchase/PurchaseConfirmQueryController/getConfirmStatus.md)
- [Trouble](../../trouble/purchase/PurchaseConfirmQueryController/getConfirmStatus.md)
