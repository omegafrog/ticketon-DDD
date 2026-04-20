# POST /api/v1/payments/confirm Trouble

## Before

- confirm은 외부 PG 호출 전에 이벤트 버전을 다시 봐야 하고, 이전에는 이 경계가 더 늦게 있었다.
- hold 기반 보호는 있었지만 외부 I/O가 길어질수록 실패 복구 비용이 커졌다.
- event sourcing / outbox / scheduler / worker 경계가 여러 번 바뀌며 책임 분리가 필요해졌다.

## Decision Points

- controller는 요청만 접수하고, worker가 expectedSalesVersion과 current version을 재검증한 뒤 PG 호출을 진행한다.
- worker가 terminal state를 만나면 retry를 멈추고, non-terminal state만 재시도한다.
- logical hold에서 DB row lock, 그리고 최종적으로 holder 제거로 concurrency 제어 전략이 바뀌었다.

## Failure Modes

- 버전 확인이 늦으면 이미 변경된 이벤트에 대해 잘못된 결제가 확정될 수 있다.
- retry가 terminal state를 무시하면 중복 승인이나 PG 과금이 반복될 수 있다.
- 락 전략이 과도하면 정상 결제보다 대기 시간이 더 길어질 수 있다.

## Why It Matters

- 결제 confirm은 가장 비싼 외부 I/O를 포함하므로, 작은 설계 차이가 곧 비용 차이로 이어진다.
- 이 문서는 history를 읽어야 현재 전략이 왜 이렇게 수렴했는지 보인다.

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
- [Troubleshooting](../../troubleshooting/purchase/PurchaseCommandController/confirmPayment.md)
