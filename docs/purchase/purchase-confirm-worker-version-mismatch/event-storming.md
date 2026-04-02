# Event Storming

## Task Traceability
- `UC-001` 대상 flow: `PurchaseCommandController.confirmPayment()` -> `PurchaseConfirmCommandService.requestConfirm()` -> `PurchaseConfirmWorker.process()`
- `UC-001` 정책 적용 위치: `PurchaseConfirmWorker` 내부 `eventServiceClient.getEventSummary(ctx.eventId)` 호출 직후

## Command
- `ConfirmPayment` (`UC-001`)
  - controller 진입점에서 confirm 요청을 수락한다.
- `ProcessPurchaseConfirm` (`UC-001`)
  - worker가 confirm payload를 소비하면서 정책 검증과 후속 결제 처리를 수행한다.

## Domain Events
- `PurchaseConfirmRequested` (`UC-001`)
  - event store에서 `CONFIRM_REQUESTED` 이벤트를 읽어 confirm context를 복원한다.
- `EventVersionMismatchedDuringConfirm` (`UC-001`)
  - `ctx.expectedSalesVersion`과 `eventSummary.version`이 다를 때 정책 위반이 감지된다.

## Policy
- `VersionMismatchPolicy` (`UC-001`)
  - 결제 처리 도중 `event.version`이 최초 기대값과 달라지면 정책 위반으로 실패해야 한다.
  - 구현상 `ctx.expectedSalesVersion.equals(eventSummary.getVersion())` 불일치 시 `ConcurrencyFailureException`이 발생한다.
  - 외부 관찰 기준은 catch/rethrow 이후의 `RuntimeException`이다.

## Read Models / External Views
- `EventSummary` (`UC-001`)
  - `eventServiceClient.getEventSummary(ctx.eventId)`가 반환하는 외부 이벤트 요약 정보다.

## Test Trace
1. `CONFIRM_REQUESTED` 이벤트가 포함된 event store 응답을 구성한다.
2. payload에 `expectedSalesVersion = 1`을 담는다.
3. `eventServiceClient.getEventSummary()`는 `version = 2`를 반환하게 stub 한다.
4. `worker.process(messageId, payloadJson)` 실행 시 정책 위반 예외를 확인한다.
5. 이후 `paymentProviderRouter`, `finalizationService`가 호출되지 않았음을 확인한다.
