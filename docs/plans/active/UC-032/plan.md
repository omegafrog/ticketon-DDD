---
change_set_id: CHG-20260625-001
contract_version: 1
doc_id: "UC-032:plan"
doc_type: plan
source_docs:
  - docs/changes/active/CHG-20260625-001.md
  - docs/use-cases/UC-032/use-case.md
  - docs/use-cases/UC-032/event-storming.md
  - docs/use-cases/UC-032/ddd-design.md
  - docs/changes/active/CHG-20260625-001.ddd-integration.md
  - docs/changes/active/CHG-20260625-001.ddd-integration.json
  - docs/use-cases/UC-032/technical-decisions.md
  - docs/use-cases/UC-032/affected-files.md
  - docs/use-cases/UC-032/e2e-goal.md
  - ARCHITECTURE.md
  - .codex/repository-settings.md
status: active
work_item_id: UC-032
---
# 구현 계획

## 1. 구현 목표
- ChangeSet: `CHG-20260625-001`
- Work item: `UC-032` (`use_case`)
- 목표: 인증된 `ADMIN` 또는 `MANAGER`가 `POST /api/v1/notifications`로 지정한 `Recipient User ID` 대상 unread `Notification` 1건을 생성하고, 생성 직후 recipient-scoped inbox 조회 경로에서 관찰 가능하게 만든다.

## 2. 구현하지 말아야 할 것
- `app/**`에 business logic, controller, repository, security policy를 추가하지 않는다.
- `notification` 모듈 패키지 taxonomy를 `ui/application/domain/infra` 외 다른 이름으로 재구성하지 않는다.
- 직접 생성 경로에 outbox, broker, scheduler, cache, retry, idempotency key, 신규 inbox entity를 추가하지 않는다.
- `domain` 또는 `application` 계층 메서드 시그니처에 servlet/web/security request type을 넣지 않는다.
- `UC-030`, `UC-031`의 조회/읽음 전이/삭제 규칙을 다시 설계하지 않는다.
- canonical 설계 문서, completed plan 경로, secret 파일을 수정하지 않는다.

## 실행 경계
- 대상 bounded context/module: `Notification Management Context` / `notification`
- 대상 aggregate root: `org.codenbug.notification.domain.entity.Notification`
- 범위 판정 기준: `docs/use-cases/UC-032/affected-files.md`의 legacy taxonomy는 현재 repo 실경로에 다음처럼 대응시킨다. `controller` -> `ui`, `application/service` -> `application`, `domain/service` -> `domain`, `infrastructure` -> `infra`.
- scope repair: `execute-work-item`의 `scope_conflict` 근거에 따라 executor 경계는 `notification/**` 수정과 notification-local 검증으로 제한한다. `scripts/run-app-*.sh` 실행은 `platform/gateway/build/**`와 build resource mirror를 생성해 재차 차단되므로 이번 work item 완료 조건에서 제외한다.
- architecture verification boundary: repo root `architectureRules`는 현재 `build.gradle`에서 `:app:architectureRules`에 위임되고, 직전 실행에서 `app/build/**`, `event/build/**`, `platform/gateway/build/**` 산출물을 만들어 declared scope를 넘었다. 이번 work item에서는 Gradle architecture task를 재실행하지 않고 `notification/src/main/java`, `notification/src/test/java` 대상 Semgrep 결과를 in-scope architecture evidence로 사용한다.
- aggregate root 파일 계약: `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`는 기존 파일 제자리 수정만 허용한다. 삭제 후 재생성, 경로 이동, 신규 entity 치환은 금지한다.

### 수정 허용 경로
- `notification/src/main/java/org/codenbug/notification/ui/NotificationCommandController.java`
- `notification/src/main/java/org/codenbug/notification/ui/TestNotificationController.java`
- `notification/src/main/java/org/codenbug/notification/ui/dto/NotificationCreateRequestDto.java`
- `notification/src/main/java/org/codenbug/notification/application/NotificationCommandService.java`
- `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`
- `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`
- `notification/src/main/java/org/codenbug/notification/infra/config/NotificationConfig.java`
- `notification/src/main/java/org/codenbug/notification/infra/event/PurchaseEventListener.java`
- `notification/src/main/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListener.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationCommandServiceIdempotencyTest.java`
- `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapterTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/event/PurchaseEventListenerTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListenerTest.java`
- `notification/src/test/java/org/codenbug/notification/ui/NotificationCommandControllerTest.java`

### 수정 금지 경로
- `app/**`
- `platform/gateway/**`
- `auth/**`
- `purchase/**`
- `seat/**`
- `event/**`
- `dispatcher/**`
- `broker/**`
- `user/**`
- `**/application-secret.yml`
- `docs/changes/active/CHG-20260625-001*`
- `docs/use-cases/**`
- `docs/plans/completed/**`

### 영향받는 기존 파일
- `notification/src/main/java/org/codenbug/notification/ui/NotificationCommandController.java`
- `notification/src/main/java/org/codenbug/notification/ui/TestNotificationController.java`
- `notification/src/main/java/org/codenbug/notification/ui/dto/NotificationCreateRequestDto.java`
- `notification/src/main/java/org/codenbug/notification/application/NotificationCommandService.java`
- `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`
- `notification/src/main/java/org/codenbug/notification/infra/config/NotificationConfig.java`
- `notification/src/main/java/org/codenbug/notification/infra/event/PurchaseEventListener.java`
- `notification/src/main/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListener.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`
- `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapterTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/event/PurchaseEventListenerTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListenerTest.java`

## 패키지 및 의존성 계약
### 생성/수정 클래스와 정확한 package
- `org.codenbug.notification.ui.NotificationCommandController`: create endpoint 유지, request DTO 검증 결과를 application 호출로 매핑.
- `org.codenbug.notification.ui.dto.NotificationCreateRequestDto`: `userId`, `type`, `title`, `content` required, `targetUrl` optional 계약 고정.
- `org.codenbug.notification.application.NotificationCommandService`: create 트랜잭션 경계, save 1회, DTO/event 변환, invalid path no-save 보장.
- `org.codenbug.notification.application.port.NotificationStore`: create/query/delete에 쓰는 persistence port. 시그니처 변경은 필요 최소로만.
- `org.codenbug.notification.domain.NotificationDomainService`: `UserId`, `NotificationContent`, unread `Notification` 생성 규칙 소유.
- `org.codenbug.notification.domain.entity.Notification`: 생성 직후 `isRead=false`와 persisted payload 일관성 유지.
- `org.codenbug.notification.domain.entity.NotificationContent`: title/content required, length rule, `targetUrl` optional validation 소유.
- `org.codenbug.notification.domain.entity.UserId`: blank recipient ID 거절과 trim 정규화 소유.
- `org.codenbug.notification.infra.NotificationStoreAdapter`: application port를 repository query/save로 연결.
- `org.codenbug.notification.infra.NotificationInboxViewReaderAdapter`: recipient-scoped inbox/unread query projection이 create 직후 최신순 가시성을 유지하도록 연결한다.
- `org.codenbug.notification.infra.NotificationRepository`: recipient-scoped inbox/unread visibility query 계약 유지.
- `org.codenbug.notification.infra.config.NotificationConfig`: 기존 `NotificationDomainService` bean wiring 유지. 새 bean 필요 시 이 경계 안에서만 추가.
- `org.codenbug.notification.ui.TestNotificationController`: 수동 테스트용 endpoint가 정식 create 경로 보안/검증 계약을 우회하지 않도록 유지한다.
- `org.codenbug.notification.infra.event.PurchaseEventListener`
- `org.codenbug.notification.infra.messaging.PurchaseNotificationEventListener`

### 각 클래스의 layer와 책임
- `ui`: HTTP endpoint, `@AuthNeeded`, `@RoleRequired`, request validation, `201` 응답 작성만 담당.
- `application`: 유스케이스 orchestration, transaction, port 호출, event publication 담당.
- `domain`: aggregate invariant, 값 객체 검증, unread 초기화, ownership/compatibility 규칙 담당.
- `infra`: JPA repository, adapter, config, listener 구현 담당.
- `ui/dto`: API contract와 application/domain 사이 직렬화 경계 담당.

### 허용 의존성 방향
- `ui`는 `application`, `ui.dto`, `platform/common Role`, `security-aop` annotation에만 의존한다.
- `application`은 `application.port`, `domain`, `ui.dto`, Spring transaction/event API에만 의존한다.
- `domain`은 `domain.entity`, JDK, 자체 도메인 helper에만 의존한다.
- `infra`는 `application.port`, `domain`, Spring Data/JPA, Spring config에만 의존한다.
- 테스트는 대상 layer와 test fixture에만 의존하고, application 테스트가 `NotificationRepository`를 직접 필드로 가지면 안 된다.

### 금지 import/framework dependency
- `domain`에 `@Service`, `@Component`, servlet, `ResponseEntity`, security annotation import 금지.
- `application`에 `NotificationRepository` 직접 주입, `LoggedInUserContext` 사용, servlet request/response 타입 사용 금지.
- `ui`에서 JPA repository 직접 호출 금지.
- `infra`에서 business rule 재구현 금지.
- 신규 `controller`, `service`, `presentation`, `infrastructure` sibling package 생성 금지.

### bootstrap/configuration wiring
- `notification/src/main/java/org/codenbug/notification/infra/config/NotificationConfig.java`의 `notificationDomainService()` bean 등록 방식을 유지한다.
- create 경로 구현 때문에 `app/` import wiring을 늘리지 않는다.
- 새 collaborator가 필요하면 `notification` 모듈 내부 component scan 또는 config bean으로 해결한다.

## 도메인 구현 계약
### Aggregate invariant
- 유효한 create 요청은 지정 recipient에 대해 unread `Notification` 정확히 1건만 저장한다.
- 비인증, 권한 부족, blank `Recipient User ID`, 필수 입력 누락 또는 blank 요청은 저장 없이 거절한다.
- `Notification Inbox`는 별도 entity가 아니라 recipient-scoped query view다.

### 상태 전이
- UC-032 create 경로는 `Notification` 생성 직후 `isRead=false`만 허용한다.
- UC-032는 `READ` 초기값, 삭제 상태, 비동기 delivery 성공 여부를 생성 완료 조건으로 사용하지 않는다.
- `status` 필드는 기존 persisted shape를 유지하되 create 성공 시 현재 생성자 초기값과 충돌시키지 않는다.

### Entity/Value Object 생성 및 검증 규칙
- `UserId`는 trim 후 빈 문자열이면 예외를 던져야 한다.
- `NotificationCreateRequestDto`는 `userId`, `type`, `title`, `content`를 request 단계에서 거절해야 한다.
- `NotificationContent`는 `title` blank 금지, `content` null 또는 blank 금지, title 100자 초과 금지, content 500자 초과 금지 규칙을 가져야 한다.
- `targetUrl`만 optional이며 null 허용이다.
- `Notification` 생성자는 `userId`, `type`, `notificationContent` null을 거절하고 unread 초기화 일관성을 유지해야 한다.

### Domain Service 여부와 책임
- `NotificationDomainService`를 유지한다.
- 책임은 recipient/value object 생성, unread `Notification` 조립, ownership validation, compatibility helper 제공이다.
- 권한 판정은 domain 책임이 아니다. controller의 security 경계를 통과한 호출만 application이 받는다고 가정한다.

### Domain Event 및 persistence compatibility
- `NotificationCommandService.createNotification(...)`는 save 후 `NotificationEventDto`를 local `ApplicationEventPublisher`로 발행할 수 있다.
- local event publish 실패를 위해 broker/outbox를 도입하지 않는다.
- JPA 스키마는 기존 `notification` 테이블을 유지한다. `content` requiredness는 우선 DTO/domain validation으로 fail-fast 보장하고, column nullability 강화는 현재 schema 증거가 있을 때만 최소 변경한다.

### 다른 Aggregate/Bounded Context 협력 방식
- `Access Control Context`는 `security-aop` annotation과 role enum으로 create 선행 권한만 보장한다.
- `Notification Management Context`는 승인된 입력만 받아 aggregate를 저장한다.
- `UC-030` 조회 경로와의 협력은 저장 후 기존 recipient-scoped inbox/unread query 결과에서 가시성을 확인하는 방식으로 제한한다.
- `PurchaseEventListener`, `PurchaseNotificationEventListener`는 stricter `NotificationContent` 규칙 이후에도 기존 생성 경로가 깨지지 않도록 compatibility만 보완한다.

### Transaction, idempotency, concurrency
- `NotificationCommandService.createNotification(...)` 하나를 synchronous `@Transactional` write boundary로 유지한다.
- direct authenticated create 경로에는 `sourceKey` 기반 idempotency를 추가하지 않는다.
- invalid input 예외는 save 전에 발생해야 하고 transaction rollback으로 종료돼야 한다.
- 동시 create dedupe 요구는 이번 UC 범위 밖이다. 요청마다 유효하면 1건 생성 가능하다.

## 외부 계약 읽기 허용 목록
- 권한 annotation 계약 확인: `security-aop/src/main/java/org/codenbug/securityaop/aop/AuthNeeded.java`
- 역할 annotation 계약 확인: `security-aop/src/main/java/org/codenbug/securityaop/aop/RoleRequired.java`
- 로그인 사용자 접근 방식 확인: `security-aop/src/main/java/org/codenbug/securityaop/aop/LoggedInUserContext.java`
- 역할 enum 확인: `platform/common/src/main/java/org/codenbug/common/Role.java`
- create 응답 DTO 매핑 확인: `notification/src/main/java/org/codenbug/notification/ui/dto/NotificationDto.java`
- local event payload 확인: `notification/src/main/java/org/codenbug/notification/ui/dto/NotificationEventDto.java`
- inbox 가시성 API 계약 확인: `notification/src/main/java/org/codenbug/notification/ui/NotificationQueryController.java`
- 런타임 기동 계약 확인: `scripts/run-app-infra.sh`
- 런타임 준비 상태 확인: `scripts/check-app-infra.sh`
- 런타임 서버 실행 계약 확인: `scripts/run-app-server.sh`
- test gate 확인: `.codex/test-gate.yaml`

## 작업 체크리스트
이 섹션의 기존 `- [x]` 표시는 이전 실행 시도의 보존 이력이다. scope repair 이후 executor는 notification 경계 안의 코드/테스트만 다루고, 검증 재실행도 `VERIFY-001`, `VERIFY-002`, `VERIFY-003`, `VERIFY-007` 범위로 제한한다.
- [ ] TASK-001 `notification/src/main/java/org/codenbug/notification/ui/NotificationCommandController.java`, `notification/src/main/java/org/codenbug/notification/ui/TestNotificationController.java`: `POST /api/v1/notifications`가 `@AuthNeeded`, `@RoleRequired({Role.ADMIN, Role.MANAGER})`, `@Valid`, `201` 응답을 유지하고 수동 테스트용 endpoint가 정본 create 보안/검증 계약을 우회하지 않도록 정리한다.
- [ ] TASK-002 `notification/src/main/java/org/codenbug/notification/ui/dto/NotificationCreateRequestDto.java`: `userId`, `type`, `title`, `content` required와 `targetUrl` optional 계약을 annotation과 메시지로 고정한다.
- [ ] TASK-003 `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`: `content` null/blank 금지와 길이 제한을 추가해 title/content validation을 VO에 집중시킨다.
- [ ] TASK-004 `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`, `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`: 기존 aggregate root 파일을 제자리 수정해 unread 초기화, recipient/value object 조립, null-safe aggregate 생성 규칙을 정리하고 invalid path가 persistence 전에 실패하도록 만든다.
- [ ] TASK-005 `notification/src/main/java/org/codenbug/notification/application/NotificationCommandService.java`: create 경로가 save 1회, DTO 반환, local event publish 1회만 수행하고 invalid input에서는 save 0회임을 보장한다.
- [ ] TASK-006 `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapter.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`: save 후 recipient-scoped inbox/unread query에서 created notification이 최신순으로 관찰되는 기존 query 계약을 유지한다.
- [ ] TASK-007 `notification/src/main/java/org/codenbug/notification/infra/event/PurchaseEventListener.java`, `notification/src/main/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListener.java`: stricter `NotificationContent` 규칙과 충돌하는 payload가 있으면 canonical create 규칙을 깨지 않는 최소 compatibility 보완만 한다.
- [ ] TEST-001 `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`: blank recipient ID, blank title, blank content, optional `targetUrl`, unread 초기 상태를 검증한다.
- [ ] TEST-002 `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`: application이 `NotificationStore` port만 사용하고 valid create에서 save/event 각 1회, invalid create에서 save 0회임을 검증한다.
- [ ] TEST-003 `notification/src/test/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapterTest.java`: create 후 persisted notification이 recipient inbox/unread query에서 최신순으로 관찰되는지 검증한다.
- [ ] TEST-004 `notification/src/test/java/org/codenbug/notification/ui/NotificationCommandControllerTest.java`: `ADMIN`/`MANAGER` 성공, `USER` 거절, 비인증 거절, request validation 실패를 controller/security 경계에서 검증한다.
- [ ] TEST-005 `notification/src/test/java/org/codenbug/notification/infra/event/PurchaseEventListenerTest.java`, `notification/src/test/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListenerTest.java`: stricter content rule 이후 기존 listener 기반 생성 경로가 깨지지 않는지 회귀를 추가한다.
- [ ] TASK-008 scope repair: `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`, repo root `architectureRules` 수정/실행은 이번 executor 경계에서 제외한다. gateway `8080` 수동 스모크와 cross-module ArchUnit 태스크는 후속 범위 확장 세션 전용으로 남기고, 현재 work item 증명은 notification 모듈 테스트, query adapter 검증, notification-targeted Semgrep 결과로 닫는다.

## 집중 검증
scope repair 이후 executor가 재실행할 검증은 notification-local 명령만 허용한다. runtime 런처 검증은 직전 시도에서 `platform/gateway/build/**`와 build resource mirror를 생성해 `scope_conflict`를 일으켰으므로 완료 게이트에서 제외한다.
- [ ] VERIFY-001 Build: `./gradlew :notification:build --no-daemon --console=plain` -> `notification` 모듈 compile/package 성공, 변경 범위 compile error 0건.
- [ ] VERIFY-002 Focused tests: `./gradlew :notification:test --no-daemon --console=plain` -> `notification` 집중 테스트 통과, 새 create/validation/visibility 회귀 포함.
- [ ] VERIFY-003 Architecture test: 현재 executor 경계에서는 N/A. repo root `./gradlew architectureRules --no-daemon --console=plain`는 `:app:architectureRules`로 위임돼 `app/build/**`, `event/build/**`, `platform/gateway/build/**` 산출물을 만들므로 재실행 금지다. 현재 architecture evidence는 `VERIFY-007`의 `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java` 결과로 대체하며, 기대 결과는 notification 범위 blocking finding 0건이다.
- [ ] VERIFY-004 E2E 또는 maintenance verification: `.harness/runs/run-51e3f91efbf4/work-items/UC-032/steps/execute-work-item/evidence/e2e.txt` -> 인증된 `ADMIN`/`MANAGER` create 성공, 실패 경로 저장 없음, recipient-scoped inbox 가시성 증거가 기록돼 있어야 한다.
- [ ] VERIFY-005 Test gate: `.codex/test-gate.yaml`의 `required: []` 확인 -> 추가 강제 stage 없이 현재 검증 묶음으로 gate 충족 기록.
- [ ] VERIFY-006 Runtime server verification: N/A - `python3 -m harness_codex run app status`, `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh` 재실행은 `platform/gateway/build/**`, `application-secret.yml` build mirror를 생성해 current execution boundary를 벗어나므로 완료 게이트에서 제외한다.
- [ ] VERIFY-007 Static analysis: `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java` -> blocking architecture finding 0건.

### 현재 실행 순서
- 1차: `./gradlew :notification:build --no-daemon --console=plain`
- 2차: `./gradlew :notification:test --no-daemon --console=plain`
- 3차: `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java`
- 실패 시: 실패 지점과 직접 연결된 검증만 국소 재실행한다. 범위 밖 런타임 명령은 다시 수행하지 않는다.

### 선택 수동 스모크
- 로컬 인증 토큰과 recipient 계정이 이미 준비됐고 별도 수동 검증 세션에서 scope 제한이 풀린 경우에만 `python3 -m harness_codex run app --foreground` 뒤 `POST /api/v1/notifications`와 recipient unread 조회를 연속 호출해 `201` 응답과 inbox 가시성을 추가 확인한다.
- 현재 executor 완료 조건은 이 수동 스모크에 의존하지 않는다. `TEST-002`, `TEST-003`, `TEST-004`, `VERIFY-001`~`VERIFY-005`, `VERIFY-007` 결과로 create 성공, 저장 없음 실패, recipient-scoped visibility를 증명한다.

### 중단 조건
- `NotificationContent` required `content` 강화가 기존 listener payload와 충돌해 `notification` 범위 내부에서 compatibility 수정을 끝낼 수 없을 때.
- create visibility 검증에 필요한 recipient query 경로가 `notification` 모듈 밖 구조 변경 없이는 성립하지 않을 때.
- 범위 밖 dirty 변경과 충돌해 `notification` create 흐름 최소 수정이 불가능할 때.
- 테스트 또는 런처가 secret/외부 승인 없이는 시작조차 불가능하고, `notification` 범위 대체 검증으로 안전성을 증명할 수 없을 때.

## 9. OWASP Security Review
- pending `security_plan_reviewer`; attack surface: `POST /api/v1/notifications` request body의 `userId`, `type`, `title`, `content`, `targetUrl`, role bypass 시도, 비인증 호출, invalid payload 저장 시도, 생성 후 타 사용자 inbox 노출 여부.

## 10. 완료 조건
- 보존된 완료 이력과 현재 scope repair 제약이 모두 정리된다. `VERIFY-006`은 범위 밖 런타임 검증 제외 사유를 명시적으로 기록한 상태여야 한다.
- 필요한 테스트가 존재하고 통과.
- Build, focused tests, E2E 또는 maintenance verification, architecture test의 현재 경계상 비적용 결정, Test gate, Runtime server verification 제외 근거, Static analysis의 현재 실행 결과가 기록된다.
- create 성공 시 unread 1건 저장과 recipient inbox 가시성이 확인된다.
- active -> completed 전이는 `complete-work-item-plan`만 수행한다.

## 11. 검증 결과
- Build: pending
- Focused tests: pending
- Architecture test: pending
- E2E 또는 maintenance verification: pending
- Test gate: pending
- Runtime server verification: pending
- Static analysis: pending
- 선택 수동 스모크: 불필요. 현재 executor 경계는 토큰 획득과 gateway launcher 부작용을 제외하므로 notification-local 테스트가 완료 증거다.
