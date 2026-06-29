---
change_set_id: CHG-20260625-001
contract_version: 1
doc_id: "UC-030:plan"
doc_type: plan
source_docs:
  - docs/changes/active/CHG-20260625-001.md
  - docs/use-cases/UC-030/use-case.md
  - docs/use-cases/UC-030/event-storming.md
  - docs/use-cases/UC-030/ddd-design.md
  - docs/changes/active/CHG-20260625-001.ddd-integration.md
  - docs/changes/active/CHG-20260625-001.ddd-integration.json
  - docs/use-cases/UC-030/technical-decisions.md
  - docs/use-cases/UC-030/affected-files.md
  - docs/use-cases/UC-030/e2e-goal.md
  - ARCHITECTURE.md
  - .codex/repository-settings.md
status: active
work_item_id: UC-030
---
# 구현 계획

## 1. 구현 목표
- ChangeSet: `CHG-20260625-001`
- Work item: `UC-030` use_case
- 목표: 인증된 `Notification Recipient`가 gateway 경유로 자기 소유 알림의 inbox 목록, unread 목록, unread count, detail 조회를 수행하고, 성공한 detail 조회에서 같은 알림 1건만 `UNREAD -> READ`로 전이되는 동작을 `notification` 모듈 안에서 관찰 가능하게 만든다.
- E2E 목표: gateway REST API에서 recipient-scoped list, unread list, unread count, detail 응답, detail 이후 단건 read 전환만 관찰 가능해야 한다.
- 입력 상태:
  - present: `docs/changes/active/CHG-20260625-001.md`, `docs/use-cases/UC-030/use-case.md`, `docs/use-cases/UC-030/event-storming.md`, `docs/use-cases/UC-030/ddd-design.md`, `docs/use-cases/UC-030/technical-decisions.md`, `docs/use-cases/UC-030/e2e-goal.md`, `docs/use-cases/UC-030/affected-files.md`, `docs/changes/active/CHG-20260625-001.ddd-integration.md`, `docs/changes/active/CHG-20260625-001.ddd-integration.json`, `ARCHITECTURE.md`, `.codex/repository-settings.md`, `notification/AGENTS.md`, `.codex/test-gate.yaml`
  - missing: 없음

## 2. 구현하지 말아야 할 것
- `app/**`, `platform/**`, 다른 bounded context 코드 수정
- `notification` 바깥 모듈로 조회 규칙, 인증 규칙, read 전이 규칙 이동
- `application`/`domain`에 servlet, Spring MVC request/response, security AOP 타입 유입
- `Notification Inbox`를 별도 저장 aggregate/entity로 재모델링
- Redis cache, outbox/inbox, queue, lock, 새 storage 기술 추가
- `/api/v1/notifications/subscribe` ADMIN SSE 경로 동작 변경
- `application-secret.yml` 및 secret 출력

## 실행 경계
- 대상 bounded context/module: `notification`
- 대상 aggregate root: `org.codenbug.notification.domain.entity.Notification`
### 수정 허용 경로
- `notification/src/main/java/org/codenbug/notification/ui/**`
- `notification/src/main/java/org/codenbug/notification/application/**`
- `notification/src/main/java/org/codenbug/notification/domain/**`
- `notification/src/main/java/org/codenbug/notification/infra/**`
- `notification/src/test/java/org/codenbug/notification/**`
- 필요 시 `notification/build.gradle`
- 필요 시 `scripts/run-app-infra.sh`
- 필요 시 `scripts/check-app-infra.sh`
- 필요 시 `scripts/run-app-server.sh`
### 수정 금지 경로
- `app/**`
- `platform/**`
- `auth/**`
- `purchase/**`
- `seat/**`
- `event/**`
- `dispatcher/**`
- `broker/**`
- `user/**`
- `**/application-secret.yml`
### 영향받는 기존 파일
- `notification/src/main/java/org/codenbug/notification/ui/NotificationQueryController.java`
- `notification/src/main/java/org/codenbug/notification/application/NotificationQueryService.java`
- `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`
- `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`
- `notification/src/main/java/org/codenbug/notification/ui/projection/NotificationListProjection.java`
- `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepository.java`
- `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepositoryImpl.java`
- `notification/src/main/java/org/codenbug/notification/infra/event/PurchaseEventListener.java`
- `notification/src/main/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListener.java`
- `notification/build.gradle`
- `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationQueryServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/ui/NotificationQueryControllerTest.java`
- `notification/src/test/java/org/codenbug/notification/ui/repository/NotificationViewRepositoryImplTest.java`
- `scripts/run-app-infra.sh`
- `scripts/check-app-infra.sh`
- `scripts/run-app-server.sh`

## 패키지 및 의존성 계약
### 생성/수정 클래스와 정확한 package
- 수정 `org.codenbug.notification.ui.NotificationQueryController`
- 수정 `org.codenbug.notification.application.NotificationQueryService`
- 수정 `org.codenbug.notification.application.port.NotificationStore`
- 생성 `org.codenbug.notification.application.port.NotificationInboxViewReader`
- 수정 `org.codenbug.notification.domain.entity.Notification`
- 수정 `org.codenbug.notification.domain.entity.UserId`
- 수정 `org.codenbug.notification.domain.entity.NotificationContent`
- 수정 `org.codenbug.notification.domain.NotificationDomainService`
- 수정 `org.codenbug.notification.infra.NotificationStoreAdapter`
- 수정 `org.codenbug.notification.infra.NotificationRepository`
- 생성 `org.codenbug.notification.infra.NotificationInboxViewReaderAdapter`
- 수정 `org.codenbug.notification.ui.projection.NotificationListProjection`
- 삭제 `org.codenbug.notification.ui.repository.NotificationViewRepository`
- 삭제 `org.codenbug.notification.ui.repository.NotificationViewRepositoryImpl`
### 각 클래스의 layer와 책임
- `ui.NotificationQueryController`: `@AuthNeeded`, `@RoleRequired({Role.USER})`, `LoggedInUserContext` 사용. `/api/v1/notifications`, `/unread`, `/count/unread`, `/{id}`만 UC-030 범위에서 조율한다. `Pageable` size를 최대 100으로 clamp하고 sort를 `sentAt DESC`로 정규화한다. `/subscribe` behavior는 바꾸지 않는다.
- `application.NotificationQueryService`: inbox, unread, unread count, detail 유스케이스 조합만 담당한다. 웹 타입을 받지 않는다. detail read transition의 트랜잭션 경계를 가진다. 주입 대상은 `NotificationStore`, `NotificationInboxViewReader`, `NotificationDomainService`로 고정한다.
- `application.port.NotificationStore`: aggregate load, save, unread count를 제공하는 write-capable application port다.
- `application.port.NotificationInboxViewReader`: recipient-scoped list와 unread projection 조회만 제공하는 read-only application port다. side effect를 금지한다.
- `domain.entity.Notification`: recipient ownership 대상 root다. unread/read 상태와 `markAsRead`, `isRead` 또는 동등한 unread 판단 API를 가진다.
- `domain.entity.UserId`: null/blank 금지, trim 기반 equality를 유지한다.
- `domain.entity.NotificationContent`: `title` 필수, 기존 길이 검증 유지, `content` required 의미 유지, `targetUrl` optional 유지.
- `domain.NotificationDomainService`: 승인된 shared 계약대로 `UC-032`용 `createNotification(...)`, `createLegacyNotification(...)` 생성 책임을 계속 가진다. UC-030 범위에서는 ownership 검증과 read 가능 여부 helper를 같은 클래스에 추가/유지할 수 있지만, 생성 책임을 제거·이관·축소하지 않는다. 조회 orchestration, repository access, transaction 제어는 맡지 않는다.
- `infra.NotificationStoreAdapter`: `NotificationStore`를 Spring Data JPA `NotificationRepository`로 위임한다.
- `infra.NotificationInboxViewReaderAdapter`: 기존 QueryDSL 조회 구현을 흡수한다. recipient scope, unread filter, `sentAt DESC`, pagination만 담당한다.
- `ui.projection.NotificationListProjection`: inbox/unread 응답 projection 표현만 담당한다. aggregate mutation 책임이 없다.
### 허용 의존성 방향
- `ui`는 `application`에만 의존한다.
- `application`은 `application.port`에 의존할 수 있다.
- `application`은 `domain`에 의존할 수 있다.
- `infra`는 `application.port`에 의존할 수 있다.
- `infra`는 `domain`에 의존할 수 있다.
- `ui.projection`은 `domain` 타입 참조가 필요할 때만 의존할 수 있다.
- `domain`은 상위 layer에 의존할 수 없다.
### 금지 import/framework dependency
- `application`과 `domain`에서 `jakarta.servlet.*`, `org.springframework.web.*`, `org.codenbug.securityaop.aop.*`, `ResponseEntity`, `RsData` import 금지
- `application`에서 `NotificationRepository` 직접 주입 금지
- `application`에서 `NotificationViewRepository`, `NotificationViewRepositoryImpl` 직접 참조 금지
- `application`에서 `ui.repository` package import 금지
- query adapter에서 `save`, `delete`, write transaction 로직 금지
- UC-030 구현을 위해 `RedisTemplate`, messaging listener, AMQP consumer, distributed lock API 추가 금지
### bootstrap/configuration wiring
- 기존 component scan과 constructor injection 유지
- `NotificationInboxViewReaderAdapter`는 `notification` 모듈 내부 Spring bean으로 등록한다.
- `NotificationQueryController`의 `NotificationEmitterService` wiring은 유지한다.
- 새 port bean wiring은 `application` 코드에서 interface만 보도록 맞춘다.

## 도메인 구현 계약
### Aggregate invariant
- 모든 조회 결과는 인증된 requester의 `userId` 범위 안에서만 노출된다.
- `Notification Inbox`는 저장 aggregate가 아니라 `Notification` 집합에서 파생된 query view다.
- unread 목록과 unread count는 상태를 바꾸지 않는다.
- 성공한 detail 조회는 조회된 동일 `Notification` 한 건만 `UNREAD -> READ` 전이 가능하다.
- non-existent 또는 foreign-owned detail 요청은 aggregate 변경 없이 실패한다.
### 상태 전이
- 생성 직후 `isRead=false`
- `getNotificationById` 성공 경로에서만 `isRead=false -> true`
- 이미 `READ`인 알림 재조회는 no-op
- repeated detail 조회는 idempotent해야 하며 다른 알림 state를 건드리면 안 된다.
### Entity/Value Object 생성 및 검증 규칙
- `UserId`: null/blank 금지, trim 유지
- `NotificationContent`: `title` 필수, 100자 초과 금지, `content` required 유지, `targetUrl`만 optional
- `Notification`: `userId`, `type`, `notificationContent` null 금지, 기존 JPA field/column 호환 유지
- persisted 표현은 boolean `isRead`를 유지하고 business 의미는 canonical `UNREAD/READ`와 호환되게 유지한다.
### Domain Service 여부와 책임
- 새 Domain Service 추가 불필요
- 기존 `NotificationDomainService`는 shared canonical 계약대로 생성 책임(`createNotification(...)`, `createLegacyNotification(...)`)을 유지한다.
- UC-030은 같은 `NotificationDomainService`에 ownership 검증과 unread 판정 helper를 추가/유지할 수 있지만, 생성 경로 동작과 시그니처를 깨면 안 된다.
- ownership 판단은 `markAsRead`보다 먼저 수행해야 한다.
### Domain Event 및 persistence compatibility
- UC-030에서 새 Domain Event 발행 금지
- `notification` 테이블 스키마와 `user_id`, `content`, `target_url`, `is_read`, `status`, `source_key` 호환성 유지
- `NotificationRepository`의 기존 삭제/상태 갱신 메서드 의미 변경 금지
### 다른 Aggregate/Bounded Context 협력 방식
- 인증과 role 판정은 gateway/security stack 선행 책임이다.
- UC-030 구현은 인증된 `userId` 문자열만 입력으로 받고 다른 bounded context API 호출을 추가하지 않는다.
- `UC-031`, `UC-032`도 같은 `NotificationAggregate`를 공유하므로 `Notification`, `UserId`, `NotificationContent`, `isRead` 의미 회귀를 막아야 한다.
### Transaction, idempotency, concurrency
- `getNotifications`, `getUnreadNotifications`, `getUnreadCount`는 `@Transactional(readOnly = true)`를 유지한다.
- `getNotificationById`만 write transaction을 사용한다.
- concurrent detail 조회는 최종 상태가 `READ`면 성공으로 간주한다.
- lock, outbox, optimistic version 도입 금지

## 외부 계약 읽기 허용 목록
- module taxonomy와 verification command 확인 -> `notification/AGENTS.md`
- required test gate 확인 -> `.codex/test-gate.yaml`
- architecture lint 규칙 확인 -> `.semgrep/ddd-architecture.yml`
- runtime smoke 계약 확인 -> `scripts/run-app-infra.sh`
- runtime smoke 계약 확인 -> `scripts/check-app-infra.sh`
- runtime smoke 계약 확인 -> `scripts/run-app-server.sh`
- shared aggregate 호환성 확인 -> `docs/changes/active/CHG-20260625-001.ddd-integration.md`
- shared aggregate 기계 판독 계약 확인 -> `docs/changes/active/CHG-20260625-001.ddd-integration.json`
- broad inventory와 실제 경로 taxonomy 충돌 해소 -> `docs/use-cases/UC-030/affected-files.md`

## 작업 체크리스트
- [ ] `notification/src/main/java/org/codenbug/notification/application/port/NotificationInboxViewReader.java`: recipient-scoped list/unread projection 전용 read port를 추가하고 count 책임은 넣지 않는다.
- [ ] `notification/src/main/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapter.java`: QueryDSL projection 조회를 이 adapter로 이동하고 recipient scope, unread filter, `sentAt DESC`, pagination 규칙을 구현한다.
- [ ] `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepository.java`: `application`이 더 이상 `ui.repository`에 의존하지 않도록 삭제한다.
- [ ] `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepositoryImpl.java`: QueryDSL 구현을 `infra.NotificationInboxViewReaderAdapter`로 이전한 뒤 삭제한다.
- [ ] `notification/src/main/java/org/codenbug/notification/ui/NotificationQueryController.java`: UC-030 대상 endpoint 4개에 `Role.USER` 보호를 유지하고 authenticated `userId` 전달, `size` 최대 100 정규화, `sentAt DESC` 보장을 구현한다. `/subscribe` ADMIN 경로는 그대로 둔다.
- [ ] `notification/src/main/java/org/codenbug/notification/application/NotificationQueryService.java`: list와 unread는 `NotificationInboxViewReader`만 사용하고, unread count는 `NotificationStore`만 사용하며, detail은 `findById -> ownership 검증 -> unread일 때만 mark/save -> dto 반환` 순서를 강제한다.
- [ ] `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`, `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`: `UC-032` 생성 책임과 기존 `createNotification(...)`/`createLegacyNotification(...)` 계약을 보존한 채 ownership 거절, unread 판단, idempotent read transition 규칙을 증명한다.
- [ ] `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`, `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`: blank 금지, trim equality, 제목/내용 검증이 shared aggregate 계약과 호환되게 유지한다.
- [ ] `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`: application이 port에만 의존하고 unread count, aggregate load/save contract를 인프라가 충족하도록 정리한다.
- [ ] `notification/src/main/java/org/codenbug/notification/ui/projection/NotificationListProjection.java`: inbox/unread 응답 projection이 recipient scope와 latest-first 결과를 표현하되 mutation 책임이 없게 유지한다.
- [ ] `notification/src/test/java/org/codenbug/notification/ui/NotificationQueryControllerTest.java`: unauthenticated 거절, `Role.USER` 보호, authenticated `userId` 전달, `size` clamp, latest-first sort normalization을 검증한다.
- [ ] `notification/src/test/java/org/codenbug/notification/application/NotificationQueryServiceTest.java`: foreign-owned/non-existent detail 거절, unread count no-save, already-read no-save, unread-owned detail save 1회, repeated detail idempotency를 검증한다.
- [ ] `notification/src/test/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapterTest.java`: cross-recipient no-leak, unread filter, latest-first pagination을 검증하는 새 infra adapter 테스트를 추가한다.
- [ ] `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`: application layer가 `NotificationStore`, `NotificationInboxViewReader` port에만 의존하고 `ui.repository` 직접 의존이 제거됐음을 검증한다.
- [ ] `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`: ownership 검증, unread 판단, read transition idempotency와 함께 shared `UC-032` 생성 경로(`createNotification(...)`, `createLegacyNotification(...)`) 회귀가 없음을 검증한다.
- [ ] `notification/src/main/java/org/codenbug/notification/infra/event/PurchaseEventListener.java`, `notification/src/main/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListener.java`, `notification/build.gradle`: UC-030 변경 후 compile/runtime 계약이 깨질 때만 최소 수정하고, 그대로면 무변경으로 유지한다.
- [ ] `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`: 실제 notification 변경으로 앱 기동 계약이 깨질 때만 최소 수정한다. 변경 없으면 untouched로 둔다.
- [ ] `docs/plans/active/UC-030/plan.md`: 구현 중 발견된 실제 생성/삭제 파일이 이 계획과 달라지면 코드 작업 전에 계획을 먼저 갱신한다.

## 집중 검증
- [ ] Build: `./gradlew --no-daemon :notification:build` -> `notification` 모듈 compile, test, QueryDSL generation 성공
- [ ] Focused tests: `./gradlew --no-daemon :notification:test --tests '*NotificationQueryControllerTest' --tests '*NotificationQueryServiceTest' --tests '*NotificationInboxViewReaderAdapterTest' --tests '*NotificationDomainServiceTest' --tests '*NotificationApplicationServicePortTest'` -> recipient scope, unread filter, unread count no-save, ownership rejection, detail read transition, port boundary 모두 통과
- [ ] Architecture test: `./gradlew --no-daemon architectureRules && semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java` -> 계층/패키지 경계 위반 없음
- [ ] E2E 또는 maintenance verification: `./gradlew --no-daemon :notification:test` -> UC-030 계약 및 shared aggregate 회귀 포함 전체 `notification` 테스트 통과
- [ ] Test gate: `N/A - .codex/test-gate.yaml`의 `required`가 비어 있어 추가 required stage 없음
- [ ] Runtime server verification: `./harness run app --timeout 120` -> infra, eureka, app, gateway health 성공 후 gateway를 통해 inbox, unread, count, detail smoke 확인
- [ ] Static analysis: `./gradlew --no-daemon :notification:compileTestJava` -> test 포함 컴파일 성공, package/import 계약 위반 부재 확인
### 중단 조건
- `notification` 외 경로 수정이 필요해진 경우
- `NotificationContent.content` nullability 또는 `isRead` persisted 표현을 바꿔야만 계약을 만족하는 경우
- detail 실패 경로에서 save/no-leak를 보장할 수 없는 구조적 결함이 발견된 경우
- runtime smoke에 Docker 또는 infra 미가용 등 환경 차단이 발생한 경우

## 9. OWASP Security Review
- 적용 범위: UC-030 조회 endpoint 4개와 detail read transition
- BOLA/API1 대응: `GET /api/v1/notifications/{id}`는 controller의 인증 보호와 service의 ownership 검증을 함께 통과해야만 detail을 반환한다. `NotificationQueryServiceTest`에 foreign-owned rejection과 save 미호출 검증을 둔다.
- Broken Access Control 대응: `/api/v1/notifications`, `/unread`, `/count/unread`, `/{id}`는 모두 `Role.USER` 보호를 유지한다. `NotificationQueryControllerTest`에서 unauthenticated, unauthorized 접근 거절을 검증한다.
- Unrestricted Resource Consumption/API4 대응: controller에서 `Pageable` size를 최대 100으로 clamp하고 sort를 `sentAt DESC`로 강제한다. `NotificationQueryControllerTest`에서 과대 size 입력이 clamp되는지 검증한다.
- Data Exposure 대응: `NotificationInboxViewReaderAdapter`는 모든 list/unread query에 recipient scope를 강제한다. `NotificationInboxViewReaderAdapterTest`에서 cross-recipient no-leak와 latest-first pagination을 검증한다.
- State Integrity 대응: `getUnreadCount`, list, unread list는 save를 호출하지 않아야 하고, detail은 owned unread 성공 경로에서만 save 1회를 허용한다. `NotificationQueryServiceTest`에서 no-save와 idempotent repeated detail을 검증한다.
- Injection/SSRF/CSRF 적용 제외: UC-030은 서버 간 외부 호출 추가가 없고 body 기반 write 입력을 도입하지 않으므로 이번 slice에서 새 SSRF, command injection, CSRF surface를 만들지 않는다. 관련 변경이 생기면 계획을 재승인한다.

## 10. 완료 조건
- 모든 체크박스가 `- [x]`.
- 필요한 테스트가 존재하고 통과.
- Build, focused tests, architecture test, E2E 또는 maintenance verification, Test gate, Runtime server verification, Static analysis 결과 기록.
- active -> completed 전이는 `complete-work-item-plan`만 수행.

## 11. 검증 결과
- Build: pending
- Focused tests: pending
- Architecture test: pending
- E2E 또는 maintenance verification: pending
- Test gate: N/A - `.codex/test-gate.yaml` `required: []`
- Runtime server verification: pending
- Static analysis: pending
