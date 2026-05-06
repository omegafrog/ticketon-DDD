# Implementation Plan

## 1. 구현 목표
- `purchase` 모듈의 결제 승인 흐름을 `confirm:{purchaseId}` 기준으로 멱등하게 만든다.
- 승인 요청 동시 접수, 같은/다른 `messageId` 중복 전달, terminal 상태 재처리, PG 재호출, 티켓 재생성, 좌석 구매 이벤트 재발행을 모두 방지한다.
- `PurchaseConfirmWorker.process`를 orchestration 수준으로 축소하고 세부 절차를 의미 단위 메서드 또는 전용 협력 객체로 분리한다.
- 이번 변경 범위의 purchase 테스트를 한글 문장형 메서드명으로 정비하고, build/test/static analysis 검증 절차를 명확히 남긴다.

## 2. 구현하지 말아야 할 것
- 저장소를 새 Spring 프로젝트나 신규 모듈로 재구성하지 않는다. 현재 Java 21 / Spring Boot / Gradle 멀티 모듈 baseline을 유지한다.
- `purchase` 범위를 벗어나는 admin, image, auth, gateway 기능 개선을 이번 계획에 포함하지 않는다.
- `app` 모듈에 비즈니스 로직, 결제 승인 중복 처리, 메시징 세부 구현을 추가하지 않는다.
- 결제 승인 중복 방지 기준을 `messageId` 단독 기준으로 축소하지 않는다.
- Toss 외 신규 결제 provider, webhook 기반 신규 승인 플로우, 환불/예매 일반 리팩터링을 이번 계획 범위에 포함하지 않는다.
- event/seat/broker 다른 BC 내부 패키지를 직접 참조하거나 수정하는 방향으로 문제를 해결하지 않는다.

## 3. 입력 문서
|문서|사용 목적|상태|
|---|---|---|
|`docs/design/요구사항.md`|FR-053~055, NFR-007~014, NFR-030 기준으로 결제 승인 비동기 처리, 중복 확정 방지, 상태 조회 요구를 확인한다.|확인|
|`docs/design/유스케이스.md`|UC-09, UC-10의 승인 접수/승인 결과 반영/상태 조회 흐름을 구현 체크리스트로 변환한다.|확인|
|`docs/design/이벤트 스토밍.md`|결제 승인 요청, 승인 실패, 이벤트 변경 충돌, 예매 만료 중 결제 취소 이벤트 흐름을 확인한다.|확인|
|`docs/design/details/index.md`|Booking & Commerce BC의 PaymentApplicationService, ReservationExpirationApplicationService, 관련 port/service 책임을 확인한다.|확인|
|`docs/design/details/도메인모델.md`|Purchase, Reservation, DeadLetterTask, PaymentApprovalPolicyService, PaymentExpirationCompensationService 규칙을 확인한다.|확인|
|`docs/design/details/어그리거트.md`|`PurchaseAggregate.acceptApprovalRequest/markApproved/markFailed/cancelBecauseReservationExpired`와 `ReservationAggregate.confirmPayment/assertEventVersionUnchanged` 제약을 확인한다.|확인|
|`docs/design/details/애플리케이션서비스.md`|UC-09 승인 접수/승인 결과 반영 오케스트레이션과 port 분리 원칙을 확인한다.|확인|
|`docs/design/details/바운디드컨텍스트.md`|Booking & Commerce BC가 결제/환불/DLQ를 함께 소유하고, seat/event/token은 port로만 협력해야 함을 확인한다.|확인|
|`docs/design/기술결정.md`|결제 승인 중복 방지, worker 추상화, Toss idempotency key, 한글 테스트 메서드명, outbox/inbox 정책을 계획에 반영한다.|확인, 이번 요청으로 purchase 범위 승인 재확인|
|`ARCHITECTURE.md`|`purchase` 모듈의 계층 경계, cross-BC port 사용, outbox/inbox, 정적 분석 명령을 executor 제약으로 사용한다.|확인|

## 4. 아키텍처 제약
- ARCHITECTURE.md 기준: 기존 `org.codenbug` Java 21 / Spring Boot / Gradle 멀티 모듈을 유지하고, 이번 변경은 `purchase` 모듈 중심으로만 점진 반영한다.
- 모듈/패키지 경계: `purchase`는 Booking/Payment BC를 담당한다. 결제 승인 접수/처리 로직은 `purchase` 내부의 `presentation -> application -> domain -> infrastructure` 경계를 따라야 한다.
- 의존성 방향: application 계층은 domain과 outbound port에만 의존하고, Toss/DB/메시징/processed-message 저장소 구현은 infrastructure가 담당한다. seat/event/admission token 협력은 `SeatInventoryPort`, `EventSnapshotPort`, `AdmissionTokenPort` 같은 명시 포트로만 수행한다.
- 금지 참조: domain은 Spring web, JPA 구현, RabbitMQ, HTTP client, Redis, 다른 feature module 내부 패키지에 의존하지 않는다. 다른 BC의 `domain`, `infra`, `ui`, `presentation` 패키지를 직접 참조하지 않는다.
- 메시징 제약: outbox 패턴을 유지하고, consumer는 중복 허용 입력을 멱등 처리해야 하며 aggregate key 기준 순서를 깨지 않아야 한다. 실패 재시도 상한은 5회다.
- 상태 전이 제약: 결제 완료, 예매 확정, 좌석 SOLD, 입장 토큰 만료는 aggregate root 메서드와 port 결과를 통해서만 반영한다. terminal 상태면 후속 PG 호출/최종화/이벤트 발행이 다시 실행되면 안 된다.
- 결제 provider 제약: 결제 provider는 Toss만 허용하며, 승인 호출에는 `confirm:{purchaseId}` idempotency key를 전달해야 한다.

## 5. 구현 범위
- 포함:
  - `PurchaseConfirmCommandService`의 멱등 승인 접수 강화
  - 승인 요청 동시성 경쟁을 막기 위한 purchase 소유 persistence/outbox 저장 경로 보강
  - `PurchaseConfirmWorker`의 중복 전달 방어와 orchestration 수준 리팩터링
  - Toss 승인 호출 idempotency key 전달
  - purchase 범위 단위/통합 테스트와 한글 문장형 메서드명 정비
- 제외:
  - 환불 기능 전반 리팩터링
  - seat/event/broker/auth 모듈의 신규 기능 추가
  - 신규 모듈 도입, build 구조 변경, CI 변경
  - purchase 범위를 벗어난 문서 갱신
- 가정:
  - `ARCHITECTURE.md`, `./gradlew architectureRules`, `.semgrep/ddd-architecture.yml`은 이미 사용 가능하다.
  - 구현 중 직접 필요한 uniqueness 보강은 purchase 소유 persistence 범위에서 해결한다.
  - 다른 BC 계약 변경 없이 purchase 내부 멱등 처리와 port 호출 제어만으로 이번 요구를 충족하는 것을 우선한다.

## 5.1 승인된 기술 결정
|영역|결정|구현 반영|테스트/검증 반영|
|---|---|---|---|
|승인 요청 멱등 접수|동일 구매의 승인 요청은 단일 `commandId=confirm:{purchaseId}`로 접수하고, 이미 접수된 요청은 새 outbox를 만들지 않는다.|`PurchaseConfirmCommandService`와 관련 저장 경로가 고정 `commandId`를 사용하고, 중복 요청은 기존 처리 상태를 돌려주는 흐름으로 정리한다.|`PurchaseConfirmCommandServiceTest`와 persistence/integration 테스트로 단일 outbox 생성과 멱등 응답을 검증한다.|
|승인 요청 동시 접수 경쟁|DB unique 제약 또는 원자적 저장으로 경쟁 상태를 방어하고, unique 충돌은 멱등 성공 응답으로 변환한다.|purchase 소유 저장소/adapter가 중복 insert race를 흡수하도록 보강한다.|동시성 또는 저장소 통합 테스트로 중복 접수 시 단일 저장만 남는지 검증한다.|
|승인 메시지 중복 수신|같은 `messageId`는 inbox/processed-message 저장 충돌로 skip하고, 다른 `messageId` 재전달도 `confirm:{purchaseId}`와 terminal event 검사로 no-op 처리한다.|worker가 PG 호출 전과 최종화 전 모두 terminal/processed guard를 적용하도록 정리한다.|`PurchaseConfirmWorkerTest`와 `PurchaseConfirmWorkerPgMockIntegrationTest`에서 same/different `messageId` 중복 전달을 검증한다.|
|PG idempotency key|Toss 승인 호출에는 `confirm:{purchaseId}`를 idempotency key로 전달한다.|Toss port 호출 인자/헤더 구성에 고정 idempotency key를 사용한다.|단위 테스트와 PG mock 통합 테스트로 동일 구매 재처리 시 같은 key가 사용되고 PG 호출이 한 번만 일어나는지 검증한다.|
|worker 추상화|`PurchaseConfirmWorker.process`는 orchestration만 남기고 메시지 검증, 중복 처리, context 로딩, 이벤트 변경 검증, PG 호출, 최종화/실패 기록을 분리한다.|private method 또는 focused collaborator로 분리해 추상화 수준을 높인다.|worker 단위 테스트가 각 분리 책임을 기준으로 읽히고, 기존 영문 테스트 메서드는 touched scope부터 한글로 바꾼다.|
|메시징/복구 정책|outbox/inbox, aggregate key ordering, 5회 재시도 후 실패 처리를 유지한다.|중복 방지 강화가 기존 retry/failure/DLQ 경로를 깨지 않도록 한다.|purchase 테스트와 `./gradlew architectureRules`, Semgrep으로 메시징/계층 규칙 회귀를 확인한다.|
|테스트 메서드명|신규/수정 테스트 메서드는 한글 문장형으로 작성한다.|이번 범위에서 수정하는 `PurchaseConfirmCommandServiceTest`, `PurchaseConfirmWorkerTest`, `PurchaseConfirmWorkerPgMockIntegrationTest` 메서드명을 정비한다.|focused tests와 코드 리뷰 체크포인트에서 한글 문장형 규칙 적용 여부를 확인한다.|

## 6. 구현 계획
- [x] `spring-package-structure` 가이드를 기준으로 `purchase` 모듈의 현재 package/layer가 `ARCHITECTURE.md`와 맞는지 먼저 검증하고, 결제 승인 중복 방지 보강 코드와 worker 분리 코드를 둘 위치를 확정한다. 신규 모듈은 만들지 않는다.
- [x] 결제 승인 구조 검증 결과를 바탕으로 purchase 계층 경계를 깨지 않는 최소 변경 전략을 정리한다. `PurchaseConfirmCommandService`, `PurchaseConfirmWorker`, processed-message 저장소, Toss port 경로 외 범위 확장은 하지 않는다.
- [x] `PurchaseConfirmCommandService`를 `confirm:{purchaseId}` 기준 멱등 접수로 보강한다. 이미 접수된 승인 요청은 새 outbox/이벤트를 만들지 않고 기존 처리 상태를 반환하도록 정리한다.
- [x] `PurchaseConfirmCommandServiceTest`에 한글 문장형 메서드명으로 중복 승인 요청이 기존 접수 결과를 재사용하고 outbox를 한 번만 만드는지 검증하는 테스트를 추가/수정한다.
- [x] 승인 요청 동시 접수 경쟁을 방어하도록 purchase 소유 persistence/outbox 저장 경로를 보강한다. DB unique 또는 원자적 저장으로 단일 `commandId`만 남기고 unique 충돌은 멱등 성공 응답으로 변환한다.
- [x] purchase 저장소/adapter 또는 통합 테스트에 한글 문장형 메서드명으로 동시 접수 시 승인 요청/outbox가 하나만 남고 재시도 요청이 실패가 아닌 멱등 성공으로 처리되는지 검증한다.
- [x] `PurchaseConfirmWorker.process`를 orchestration 수준으로 축소한다. 메시지 검증, processed-message 기록, confirm context 로딩, terminal no-op 판단, 이벤트 버전/만료 점검, PG 호출, 성공/실패 최종화를 private method 또는 focused collaborator로 분리한다.
- [x] `PurchaseConfirmWorkerTest`의 직접 수정 범위 메서드명을 한글 문장형으로 바꾸고, 같은 `messageId` 중복 전달, 다른 `messageId` 재전달, terminal 상태 선검사, PG 호출 전 조기 종료를 각각 검증하는 테스트를 추가/수정한다.
- [x] worker 중복 전달 방어를 hardening한다. same `messageId`는 inbox 충돌로 skip하고, different `messageId`라도 같은 구매의 terminal 상태 또는 동일 `commandId` 처리 완료가 확인되면 PG 재승인, 티켓 재생성, 좌석 구매 이벤트 재발행 없이 정상 no-op 처리되게 한다.
- [x] `PurchaseConfirmWorkerPgMockIntegrationTest`에 한글 문장형 메서드명으로 duplicate delivery 시 PG confirm 호출 수가 1회로 유지되고 구매 최종화/후속 발행이 중복되지 않는지 검증하는 테스트를 추가/수정한다.
- [x] Toss 승인 호출 경로에 `confirm:{purchaseId}` idempotency key 전달을 보강하고, 기존 5회 재시도/실패 기록 정책을 깨지 않도록 정리한다.
- [x] unit/integration 테스트에 한글 문장형 메서드명으로 Toss idempotency key 전달, 예매 만료 보상, 이벤트 버전 충돌 시 중복 최종화가 일어나지 않는지 검증하는 케이스를 추가/수정한다.
- [x] 리팩터링 후 purchase 결제 승인 흐름을 범위 검토한다. terminal-event no-op이 PG 호출 전과 finalization 전 둘 다 적용되는지, 기존 성공 경로와 실패/DLQ 경로가 유지되는지 점검 메모를 남긴다.

## 7. 테스트 계획
- [x] Domain/Aggregate/VO 테스트: `PurchaseAggregate`, `ReservationAggregate`의 상태 전이가 중복 승인 방지 규칙을 깨지 않는지 검증하고, 필요 시 새 VO/식별자 래퍼가 생기면 생성 규칙을 테스트한다.
- [x] Application Service 흐름 테스트: `PurchaseConfirmCommandService`와 `PurchaseConfirmWorker`가 repository/port를 올바른 순서로 호출하고, 중복 요청/중복 메시지/terminal 상태에서 멱등 no-op 또는 기존 결과 재사용으로 끝나는지 검증한다.
- [x] Infrastructure/Adapter 테스트: processed-message 저장소, outbox 저장 경로, Toss port idempotency key 전달, DB unique 충돌 변환을 검증한다.
- [x] Communication/Transaction 테스트: same/different `messageId` duplicate delivery, 단일 PG confirm, 단일 구매 최종화, 단일 티켓 생성/좌석 구매 이벤트 발행, 이벤트 버전 충돌/예매 만료 보상 경로를 검증한다.
- [x] 테스트 네이밍 규칙: 이번 범위에서 새로 작성하거나 직접 수정하는 purchase 테스트 메서드명은 모두 한글 문장형으로 맞춘다.

## 8. 검증 방법
- [x] Build:
  - 명령: `./gradlew :purchase:build --no-daemon`
  - 성공 기준: `purchase` 모듈과 직접 연관된 의존 모듈 compile/test/package가 성공하고, 멱등성 보강으로 인한 컴파일/매핑 오류가 없다.
- [x] Tests:
  - 명령: `./gradlew :purchase:test --tests org.codenbug.purchase.app.es.PurchaseConfirmCommandServiceTest --tests org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest --tests org.codenbug.purchase.integration.PurchaseConfirmWorkerPgMockIntegrationTest --no-daemon`
  - 명령: `./gradlew :purchase:test --no-daemon`
  - 성공 기준: targeted tests와 전체 `purchase` 테스트가 모두 통과하고, duplicate approval/no-op/idempotency key 관련 신규·수정 테스트 메서드명이 한글 문장형이다.
- [x] Static analysis:
  - 절차: 저장소에 이미 존재하는 ArchUnit/Semgrep 절차를 사용한다. purchase 범위 계층 위반, cross-BC 직접 참조, domain의 infra 의존, application의 구현체 직접 의존이 없는지 확인한다.
  - 명령: `./gradlew architectureRules --no-daemon`
  - 명령: `semgrep --config .semgrep/ddd-architecture.yml purchase/src/main/java`
  - 성공 기준: purchase 관련 계층/경계 위반이 0건이다. 저장소의 기존 unrelated 실패가 있으면 계획을 active 상태로 유지하고 `## 11. 검증 실패`에 분리 기록한다.

## 9. 완료 조건
- 모든 체크박스가 `- [x]` 상태다.
- 구현 범위의 테스트가 작성되어 통과했다.
- Build, Tests, Static analysis가 성공했다.
- 성공 후 `docs/plans/complete/plan.md`로 이동한다.

## 10. 검증 결과
- Build:
  - 2026-04-29 `./gradlew :purchase:build --no-daemon` 성공
  - 결과: `purchase` compile/test/package 성공
- Tests:
  - 2026-04-29 `./gradlew :purchase:test --tests org.codenbug.purchase.app.es.PurchaseConfirmCommandServiceTest --tests org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest --tests org.codenbug.purchase.integration.PurchaseConfirmWorkerPgMockIntegrationTest --no-daemon` 성공
  - 2026-04-29 `./gradlew :purchase:test --no-daemon` 성공
  - 결과: purchase 전체 테스트 통과, 이번 범위 직접 수정/추가 테스트 메서드 한글 문장형 적용 확인
- Static analysis:
  - 2026-04-29 `./gradlew architectureRules --no-daemon` 성공
  - 2026-04-29 `HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml purchase/src/main/java` 성공
  - 결과: purchase 대상 72개 파일, 6개 java rule, findings 0건
  - 참고: sandbox 내부 Semgrep은 0 findings 요약 출력 후 종료가 지연되어 동일 명령을 escalated로 재실행해 exit code 0을 확인했다.
- Runtime server verification:
  - 미적용. 이번 active plan의 `8. 검증 방법`에 서버 기동/API 런타임 검증이 정의되어 있지 않고, 범위는 purchase build/test/static analysis로 한정된다.

## 11. 검증 실패
- 없음
