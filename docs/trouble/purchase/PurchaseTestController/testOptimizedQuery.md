# GET /api/test/purchase/optimized/{eventId} Trouble

## Before

- purchase는 외부 PG, 이벤트 소싱, 재시도, 락 전략이 함께 얽혀 있어 가장 흔들리기 쉬운 도메인이다.
- confirm/refund/query/test 엔드포인트는 각각 다른 실패 경계를 가진다.
- 과거 hold/lock/worker 변경 이력이 많아, 현재 전략이 왜 이렇게 남았는지 문서에 남길 필요가 크다.

## Decision Points

- 외부 결제는 가급적 worker/command service 뒤로 밀고 controller는 얇게 유지한다.
- 버전 검증은 외부 PG 호출 전에 끝내고, terminal state는 retry하지 않는다.
- query와 command, finalization은 서로 다른 책임으로 본다.

## Failure Modes

- confirm 경계가 늦으면 잘못된 PG 승인이나 중복 결제가 발생할 수 있다.
- retry가 terminal 상태를 무시하면 PG 비용이 반복된다.
- 락이 너무 넓으면 정상 트래픽까지 병목된다.

## Why It Matters

- purchase는 비용과 정합성 둘 다 민감해서 작은 설계 변화가 바로 장애로 보인다.

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `a49d73c` (2025-09-10): refactor: 서비스 계층에서 독립된 redislock 모듈로 import 변경
- [controller] `23eec6e` (2025-08-21): feat: Purchase 도메인 QueryDSL 최적화 및 N+1 문제 해결



## Related Docs

- [Use Case](../../usecase/purchase/PurchaseTestController/testOptimizedQuery.md)
- [Flow](../../flow/purchase/PurchaseTestController/testOptimizedQuery.md)
- [Troubleshooting](../../troubleshooting/purchase/PurchaseTestController/testOptimizedQuery.md)
