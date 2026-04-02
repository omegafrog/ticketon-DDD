# Use Cases

## UC-001 결제 confirm 처리 중 event.version 변경 정책 위반 감지

### Goal
- `PurchaseConfirmWorker.process()`가 `expectedSalesVersion`과 외부 이벤트 요약의 `version` 불일치를 정책 위반으로 처리하는지 unit test로 검증한다.

### Primary Actor
- 내부 confirm worker 실행 흐름

### Entry Point
- `PurchaseCommandController.confirmPayment()`

### Preconditions
- confirm flow가 `PurchaseConfirmCommandService.requestConfirm()`를 통해 시작된다.
- `loadConfirmContext()`가 정상 동작하도록 `eventStoreRepository.findByPurchaseIdOrderByIdAsc()`에서 최소 1개의 `CONFIRM_REQUESTED` 이벤트를 반환한다.
- `tryMarkProcessed()`를 통과시키기 위해 `processedMessageRepository.save()`는 정상 반환으로 둔다.

### Main Flow
1. `PurchaseConfirmWorkerTest`가 fully mocked 외부 의존성으로 worker를 생성한다.
2. 테스트 입력으로 `CONFIRM_REQUESTED` 이벤트가 포함된 event store 응답을 구성한다.
3. payload 안에 `expectedSalesVersion`을 명시한다. 예: `1`
4. `eventServiceClient.getEventSummary()`는 동일 `eventId`에 대해 다른 `version`을 반환하도록 stub 한다. 예: `2`
5. `worker.process(messageId, payloadJson)`를 실행한다.
6. 현재 구현상 `ConcurrencyFailureException`을 catch 후 `RuntimeException`으로 다시 던지므로, 외부 관찰 기준은 `RuntimeException`이다.
7. 메시지에 “결제 도중 상품 내용이 변경되었습니다.” 포함 여부를 확인한다.

### Alternate Flow
- `PurchaseConfirmWorker` 생성자에서 `eventServiceClient` 주입 누락으로 NPE가 발생하면, 테스트 추가만으로는 불가능하므로 생성자 대입 버그 수정이 최소 범위 후속 작업으로 필요하다.

### Postconditions
- version mismatch 시 실패한다.
- PG confirm 단계 진입 전 실패해야 하므로 `paymentProviderRouter` 상호작용이 없다.
- `finalizationService` 상호작용이 없다.

### Non-Goals
- 성공 케이스 추가 안 함
- outbox/projection 저장 상세 검증 확장 안 함
- integration test 변경 안 함
