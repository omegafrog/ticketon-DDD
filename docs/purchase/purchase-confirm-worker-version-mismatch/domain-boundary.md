# Domain Boundary

## Scope
- 이번 작업은 `purchase` 모듈 내부의 confirm flow unit test 추가 계획만 다룬다.
- 1차 검증 대상은 `PurchaseConfirmWorker.process()`다.
- 검증 규칙은 하나다.
  - 결제 처리 도중 `event.version`이 최초 기대값과 달라지면 정책 위반으로 실패해야 한다.

## In Scope Components
- controller 진입점: `PurchaseCommandController.confirmPayment()`
- 요청 수락/아웃박스 적재: `PurchaseConfirmCommandService.requestConfirm()`
- 실제 정책 검증/실행: `PurchaseConfirmWorker.process()`
- 규칙 enforcement 위치:
  - `purchase/src/main/java/org/codenbug/purchase/app/es/PurchaseConfirmWorker.java`
  - `eventServiceClient.getEventSummary(ctx.eventId)` 호출 후
  - `ctx.expectedSalesVersion.equals(eventSummary.getVersion())` 불일치 시 `ConcurrencyFailureException` 발생

## External Dependencies To Mock
- `PlatformTransactionManager`
- `JpaPurchaseProcessedMessageRepository`
- `JpaPurchaseEventStoreRepository`
- `PurchaseEventAppendService`
- `EventServiceClient`
- `PaymentProviderRouter`
- `PurchasePaymentFinalizationService`

## Out of Scope
- `PurchaseConfirmCommandServiceTest` 확장
- 성공 케이스 추가
- outbox/projection 저장 상세 검증 확장
- integration test 변경
- `purchase` 외 다른 모듈의 설계 변경

## Boundary Decision
- 이번 요구사항의 핵심은 “controller가 사용하는 flow class” 전체 중 실제 정책 위반이 발생하는 지점을 검증하는 것이다.
- 따라서 신규 unit test의 1차 대상은 `PurchaseConfirmWorker`가 맞다.
- `PurchaseConfirmCommandServiceTest`는 confirm 요청 생성까지만 다루며, `event.version` 변경 실패 규칙은 여기서 검증되지 않는다.
