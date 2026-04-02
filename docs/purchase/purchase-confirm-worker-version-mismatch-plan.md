# PurchaseConfirmWorker event.version mismatch unit test plan

## Properties
status: completed

owner: plan-writer

domain: purchase

last_verified: 2026-04-02 (`:purchase:test` targeted `PurchaseConfirmWorkerTest` passed with workspace-local `GRADLE_USER_HOME`)

## Task Summary
- 이 문서는 `PurchaseCommandController.confirmPayment()`가 시작하는 confirm flow 중 `PurchaseConfirmWorker.process()`에서 수행하는 event version 정책 위반 검증용 unit test 추가 계획을 정리한다.
- 단일 검증 포인트만 다룬다.
  - 결제 처리 도중 `event.version`이 최초 기대값과 달라지면 정책 위반으로 실패해야 한다.

## Domain Boundary
- 범위 및 비범위 고정 문서: [docs/purchase/purchase-confirm-worker-version-mismatch/domain-boundary.md](docs/purchase/purchase-confirm-worker-version-mismatch/domain-boundary.md)

## Use Cases
- 단일 use case 정의 문서: [docs/purchase/purchase-confirm-worker-version-mismatch/use-cases.md](docs/purchase/purchase-confirm-worker-version-mismatch/use-cases.md)

## Event Storming
- command, event, policy, read model traceability 문서: [docs/purchase/purchase-confirm-worker-version-mismatch/event-storming.md](docs/purchase/purchase-confirm-worker-version-mismatch/event-storming.md)

## Detailed Design
- ports, adapters, interface signatures, key DTOs, test points 문서: [docs/purchase/purchase-confirm-worker-version-mismatch/detailed-design.md](docs/purchase/purchase-confirm-worker-version-mismatch/detailed-design.md)

## Flow under test
- controller 진입점: `PurchaseCommandController.confirmPayment()`
- 요청 수락/아웃박스 적재: `PurchaseConfirmCommandService.requestConfirm()`
- 실제 정책 검증/실행: `PurchaseConfirmWorker.process()`
- 규칙 enforcement 위치:
  - `purchase/src/main/java/org/codenbug/purchase/app/es/PurchaseConfirmWorker.java`
  - `eventServiceClient.getEventSummary(ctx.eventId)` 호출 후
  - `ctx.expectedSalesVersion.equals(eventSummary.getVersion())` 불일치 시 `ConcurrencyFailureException` 발생

## Decision
- 이번 요구사항의 핵심은 “controller가 사용하는 flow class” 전체 중 실제 정책 위반이 발생하는 지점을 검증하는 것이다.
- 따라서 신규 unit test의 1차 대상은 `PurchaseConfirmWorker`다.
- `PurchaseConfirmCommandServiceTest`는 confirm 요청 생성까지만 다루며, `event.version` 변경 실패 규칙은 여기서 검증되지 않는다.

## Implementation Plan
1. `purchase/src/test/java/org/codenbug/purchase/app/es/` 아래에 `PurchaseConfirmWorkerTest`를 추가한다.
2. `PurchaseConfirmWorker` 생성에 필요한 외부 의존성을 전부 mock 한다.
   - `PlatformTransactionManager`
   - `JpaPurchaseProcessedMessageRepository`
   - `JpaPurchaseEventStoreRepository`
   - `PurchaseEventAppendService`
   - `EventServiceClient`
   - `PaymentProviderRouter`
   - `PurchasePaymentFinalizationService`
3. 테스트 입력으로 `CONFIRM_REQUESTED` 이벤트가 포함된 event store 응답을 구성한다.
   - payload 안에 `expectedSalesVersion`을 명시한다. 예: `1`
4. `eventServiceClient.getEventSummary()`는 동일 eventId에 대해 다른 `version`을 반환하도록 stub 한다. 예: `2`
5. `worker.process(messageId, payloadJson)` 실행 시 예외가 발생하는지 검증한다.
   - 현재 구현상 `ConcurrencyFailureException`을 catch 후 `RuntimeException`으로 다시 던지므로, 외부 관찰 기준은 `RuntimeException`
   - 메시지에 “결제 도중 상품 내용이 변경되었습니다.” 포함 여부까지 확인한다.
6. 부수효과가 더 진행되지 않는지도 함께 검증한다.
   - PG confirm 단계 진입 전 실패해야 하므로 `paymentProviderRouter` 상호작용 없음
   - `finalizationService` 상호작용 없음
7. 테스트 이름은 규칙 중심으로 둔다.
   - 예: `process_whenEventVersionChanges_throwsPolicyViolation`

## Test design notes
- `loadConfirmContext()`가 정상 동작하도록 `eventStoreRepository.findByPurchaseIdOrderByIdAsc()`에서 최소 1개의 `CONFIRM_REQUESTED` 이벤트를 반환해야 한다.
- `tryMarkProcessed()`를 통과시키기 위해 `processedMessageRepository.save()`는 정상 반환으로 둔다.
- 트랜잭션 래핑이 있어도 단위 테스트에서는 mock `PlatformTransactionManager`로 충분하다.
- 이 테스트는 “version mismatch 시 실패” 하나만 본다.
  - 성공 케이스 추가 안 함
  - outbox/projection 저장 상세 검증 확장 안 함
  - integration test 변경 안 함

## Verification Plan
- 실행 명령: `./gradlew :purchase:test --tests org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest`
- 기대 결과: `PurchaseConfirmWorkerTest`가 `event.version` mismatch 시 `RuntimeException`과 정책 위반 메시지를 검증하고, `paymentProviderRouter`와 `finalizationService` 상호작용 부재를 확인한다.

## Implementation Result
- `PurchaseConfirmWorker` 생성자에서 `EventServiceClient` 주입값이 유지되도록 constructor wiring을 수정했다.
- `purchase/src/test/java/org/codenbug/purchase/app/es/PurchaseConfirmWorkerTest.java`를 추가했다.
- 신규 테스트 `process_whenEventVersionChanges_throwsPolicyViolation`는 version mismatch 시 `RuntimeException`과 정책 위반 메시지를 검증하고, `PG_CONFIRM_REQUESTED` 이후 단계 및 `finalizationService` 미진입을 검증한다.
- 실제 worktree diff에는 이번 문서 범위를 벗어난 `purchase/src/main/java/org/codenbug/purchase/app/PurchaseService.java` 삭제도 함께 포함되어 있다.
- 해당 삭제는 본 task의 구현/검증 범위에 포함되지 않았으며, closeout 전 별도 분리 또는 의도 확인이 필요하다.

## Verification Status
- 실행 명령: `./gradlew :purchase:test --tests org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest --no-daemon --console=plain`
- workspace-local `GRADLE_USER_HOME`로 targeted verification을 실행했다.
- 결과 파일: `purchase/build/test-results/test/TEST-org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest.xml`
- 최종 결과: `tests=1, failures=0, errors=0`
- 실행된 테스트 케이스: `process_whenEventVersionChanges_throwsPolicyViolation()`
- 테스트 stderr에는 JDK 21 환경의 Mockito inline mock maker self-attachment warning이 있었지만, 테스트는 통과했다.

## Documentation Plan
- 이 문서를 task entry point이자 단일 계획 허브로 유지한다.
- 범위 및 비범위는 [docs/purchase/purchase-confirm-worker-version-mismatch/domain-boundary.md](docs/purchase/purchase-confirm-worker-version-mismatch/domain-boundary.md)에서 고정한다.
- use case는 [docs/purchase/purchase-confirm-worker-version-mismatch/use-cases.md](docs/purchase/purchase-confirm-worker-version-mismatch/use-cases.md)에서 `UC-001`로 고정한다.
- traceability는 [docs/purchase/purchase-confirm-worker-version-mismatch/event-storming.md](docs/purchase/purchase-confirm-worker-version-mismatch/event-storming.md)에서 유지한다.
- 설계 상세는 [docs/purchase/purchase-confirm-worker-version-mismatch/detailed-design.md](docs/purchase/purchase-confirm-worker-version-mismatch/detailed-design.md)에서 유지한다.
- 보조 문서로 [docs/purchase/purchase-confirm-worker-version-mismatch/documentation-plan.md](docs/purchase/purchase-confirm-worker-version-mismatch/documentation-plan.md)를 유지하되, 문서 배치의 단일 소스 오브 트루스는 본 섹션으로 본다.

## Output Files
- 수정: `purchase/src/main/java/org/codenbug/purchase/app/es/PurchaseConfirmWorker.java`
- 신규: `purchase/src/test/java/org/codenbug/purchase/app/es/PurchaseConfirmWorkerTest.java`
- 삭제됨(비범위 worktree 변경): `purchase/src/main/java/org/codenbug/purchase/app/PurchaseService.java`

## Risks and follow-up
- 검증은 targeted command만 수행했고, 전체 `:purchase:test` suite는 아직 실행하지 않았다.
- 현재 worktree에는 이번 purchase 범위와 무관한 변경이 남아 있다.
- 특히 `purchase/src/main/java/org/codenbug/purchase/app/PurchaseService.java` 삭제가 현재 diff에 포함되어 있어, 이 task와 분리되지 않으면 PR 범위가 혼합된다.

## Raw Oracle Output
```text
## 목표
- `purchase` 모듈의 결제 confirm flow에서 외부 의존성을 모두 mock 처리한 unit test를 추가한다.
- 검증 포인트는 하나만 둔다.
  - 결제 처리 도중 `event.version`이 최초 기대값과 달라지면 정책 위반으로 실패해야 한다.

## 대상 식별
- controller 진입점: `PurchaseCommandController.confirmPayment()`
- controller가 시작하는 flow:
  - 요청 수락/아웃박스 적재: `PurchaseConfirmCommandService.requestConfirm()`
  - 실제 정책 검증/실행: `PurchaseConfirmWorker.process()`
- 규칙 enforcement 위치:
  - `purchase/src/main/java/org/codenbug/purchase/app/es/PurchaseConfirmWorker.java`
  - `eventServiceClient.getEventSummary(ctx.eventId)` 호출 후
  - `ctx.expectedSalesVersion.equals(eventSummary.getVersion())` 불일치 시 `ConcurrencyFailureException` 발생

## 판단
- 이번 요구사항의 핵심은 “controller가 사용하는 flow class” 전체 중 실제 정책 위반이 발생하는 지점을 검증하는 것이다.
- 따라서 신규 unit test의 1차 대상은 `PurchaseConfirmWorker`가 맞다.
- `PurchaseConfirmCommandServiceTest`는 confirm 요청 생성까지만 다루며, `event.version` 변경 실패 규칙은 여기서 검증되지 않는다.

## 구현 계획
1. `purchase/src/test/java/org/codenbug/purchase/app/es/` 아래에 `PurchaseConfirmWorkerTest`를 추가한다.
2. `PurchaseConfirmWorker` 생성에 필요한 외부 의존성을 전부 mock 한다.
   - `PlatformTransactionManager`
   - `JpaPurchaseProcessedMessageRepository`
   - `JpaPurchaseEventStoreRepository`
   - `PurchaseEventAppendService`
   - `EventServiceClient`
   - `PaymentProviderRouter`
   - `PurchasePaymentFinalizationService`
3. 테스트 입력으로 `CONFIRM_REQUESTED` 이벤트가 포함된 event store 응답을 구성한다.
   - payload 안에 `expectedSalesVersion`을 명시한다. 예: `1`
4. `eventServiceClient.getEventSummary()`는 동일 eventId에 대해 다른 `version`을 반환하도록 stub 한다. 예: `2`
5. `worker.process(messageId, payloadJson)` 실행 시 예외가 발생하는지 검증한다.
   - 현재 구현상 `ConcurrencyFailureException`을 catch 후 `RuntimeException`으로 다시 던지므로, 외부 관찰 기준은 `RuntimeException`
   - 메시지에 “결제 도중 상품 내용이 변경되었습니다.” 포함 여부까지 확인한다.
6. 부수효과가 더 진행되지 않는지도 함께 검증한다.
   - PG confirm 단계 진입 전 실패해야 하므로 `paymentProviderRouter` 상호작용 없음
   - `finalizationService` 상호작용 없음
7. 테스트 이름은 규칙 중심으로 둔다.
   - 예: `process_whenEventVersionChanges_throwsPolicyViolation`

## 테스트 설계 메모
- `loadConfirmContext()`가 정상 동작하도록 `eventStoreRepository.findByPurchaseIdOrderByIdAsc()`에서 최소 1개의 `CONFIRM_REQUESTED` 이벤트를 반환해야 한다.
- `tryMarkProcessed()`를 통과시키기 위해 `processedMessageRepository.save()`는 정상 반환으로 둔다.
- 트랜잭션 래핑이 있어도 단위 테스트에서는 mock `PlatformTransactionManager`로 충분하다.
- 이 테스트는 “version mismatch 시 실패” 하나만 본다.
  - 성공 케이스 추가 안 함
  - outbox/projection 저장 상세 검증 확장 안 함
  - integration test 변경 안 함

## 예상 수정 파일
- 신규: `purchase/src/test/java/org/codenbug/purchase/app/es/PurchaseConfirmWorkerTest.java`

## 검증
- `./gradlew :purchase:test --tests org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest`

## 리스크 / 확인 필요
- `PurchaseConfirmWorker` 생성자에서 `this.eventServiceClient = null;`로 주입 누락처럼 보이는 코드가 있어, 테스트 작성 시 바로 NPE가 드러날 수 있다.
- 이 경우 테스트 추가만으로는 불가능하고, 생성자 대입 버그 수정이 최소 범위로 함께 필요하다.
```
