# Detailed Design

## Test Target
- 신규 파일: `purchase/src/test/java/org/codenbug/purchase/app/es/PurchaseConfirmWorkerTest.java`
- 테스트 이름: `process_whenEventVersionChanges_throwsPolicyViolation`

## Ports
- `EventServiceClient`
  - 역할: `ctx.eventId` 기준 외부 이벤트 요약 조회
- `PaymentProviderRouter`
  - 역할: PG confirm 단계 라우팅
- `PurchasePaymentFinalizationService`
  - 역할: confirm 완료 후 후속 처리
- `PlatformTransactionManager`
  - 역할: worker 내부 트랜잭션 래핑 지원

## Adapters
- `JpaPurchaseProcessedMessageRepository`
  - 역할: `tryMarkProcessed()` 통과를 위한 처리 이력 저장
- `JpaPurchaseEventStoreRepository`
  - 역할: `findByPurchaseIdOrderByIdAsc()`로 `CONFIRM_REQUESTED` 이벤트 조회
- `PurchaseEventAppendService`
  - 역할: worker 후속 이벤트 적재 경계이지만, 이번 테스트에서는 정책 위반 전 단계라 실제 진입을 기대하지 않는다

## Interface Signatures
- `PurchaseConfirmWorker.process(messageId, payloadJson)`
- `eventStoreRepository.findByPurchaseIdOrderByIdAsc(purchaseId)`
- `processedMessageRepository.save(processedMessage)`
- `eventServiceClient.getEventSummary(ctx.eventId)`

## Key DTOs
- confirm payload JSON
  - `expectedSalesVersion` 포함. 예: `1`
- event summary DTO
  - `version` 포함. 예: `2`
- event store event
  - `CONFIRM_REQUESTED` 타입 포함

## Execution Plan
1. `PurchaseConfirmWorker` 생성에 필요한 외부 의존성을 전부 mock 한다.
2. event store repository가 최소 1개의 `CONFIRM_REQUESTED` 이벤트를 반환하도록 구성한다.
3. processed message repository save는 정상 반환으로 둔다.
4. `eventServiceClient.getEventSummary()`가 다른 version을 반환하도록 stub 한다.
5. `worker.process(messageId, payloadJson)` 실행 결과 `RuntimeException`을 검증한다.
6. 예외 메시지에 “결제 도중 상품 내용이 변경되었습니다.” 포함 여부를 검증한다.
7. `paymentProviderRouter`, `finalizationService` 상호작용 없음을 검증한다.

## Test Points
- version mismatch 시 실패
- 외부 관찰 예외 타입은 `RuntimeException`
- 정책 위반 메시지 포함
- PG confirm 단계 미진입
- finalization 미진입

## Implemented Changes
- `PurchaseConfirmWorker` 생성자에서 `EventServiceClient`가 `null`로 소실되지 않도록 주입값 유지로 수정됐다.
- 신규 테스트는 `PROCESSING_STARTED`까지는 기록되지만, version mismatch 후 `PG_CONFIRM_REQUESTED` 이후 단계로 진행하지 않음을 검증한다.

## Verification Status
- 실행 명령: `./gradlew :purchase:test --tests org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest --no-daemon --console=plain`
- workspace-local `GRADLE_USER_HOME`로 targeted verification을 실행했다.
- 결과 파일: `purchase/build/test-results/test/TEST-org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest.xml`
- 최종 결과: `tests=1, failures=0, errors=0`
- 실행된 테스트 케이스: `process_whenEventVersionChanges_throwsPolicyViolation()`
- 테스트 stderr에는 JDK 21 환경의 Mockito inline mock maker self-attachment warning이 있었지만, 테스트는 통과했다.
