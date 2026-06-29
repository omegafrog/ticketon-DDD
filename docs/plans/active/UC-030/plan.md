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
  - docs/use-cases/UC-030/technical-decisions.md
  - docs/use-cases/UC-030/e2e-goal.md
  - docs/use-cases/UC-030/affected-files.md
  - docs/changes/active/CHG-20260625-001.ddd-integration.md
  - ARCHITECTURE.md
  - .codex/repository-settings.md
status: active
work_item_id: UC-030
---
# 구현 계획

## 1. 구현 목표
- ChangeSet: `CHG-20260625-001`
- Work item: `UC-030` use_case
- 목표: 인증된 사용자에 대해 `notification` 모듈이 recipient-scoped inbox 목록, unread 목록, unread count, detail 조회를 제공하고, 성공한 detail 조회에서 조회된 동일 `Notification` 1건만 `READ`로 전이되게 만든다.
- 비즈니스 성공 기준: 목록은 최신순 페이지네이션, unread 목록/개수는 상태 불변, detail 성공 시 해당 건만 읽음 처리.
- 입력 상태:
  - present: `docs/changes/active/CHG-20260625-001.md`, `docs/use-cases/UC-030/use-case.md`, `docs/use-cases/UC-030/event-storming.md`, `docs/use-cases/UC-030/ddd-design.md`, `docs/use-cases/UC-030/technical-decisions.md`, `docs/use-cases/UC-030/e2e-goal.md`, `docs/use-cases/UC-030/affected-files.md`, `docs/changes/active/CHG-20260625-001.ddd-integration.md`, `ARCHITECTURE.md`, `.codex/repository-settings.md`, `notification/AGENTS.md`, `.codex/test-gate.yaml`
  - missing: 없음

## 2. 구현하지 말아야 할 것
- `notification` 밖 모듈 수정
- `app/` 또는 `platform/`에 알림 조회 비즈니스 로직 추가
- `application`/`domain`에 `LoggedInUserContext`, servlet, controller 타입 전달
- 메시징, Redis cache, lock, outbox 같은 새 인프라 추가
- `Notification` 저장 스키마를 이번 슬라이스에서 호환 불가하게 변경
- `/api/v1/notifications/subscribe` ADMIN SSE 동작 변경
- `application-secret.yml` 읽기, 출력, 수정

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
- `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`
- `notification/src/main/java/org/codenbug/notification/ui/projection/NotificationListProjection.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationQueryServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/ui/NotificationQueryControllerTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/event/PurchaseEventListenerTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListenerTest.java`

## 패키지 및 의존성 계약
### 생성/수정 클래스와 정확한 package
- 수정 `org.codenbug.notification.ui.NotificationQueryController`
- 수정 `org.codenbug.notification.application.NotificationQueryService`
- 수정 `org.codenbug.notification.application.port.NotificationStore`
- 생성 또는 복구 `org.codenbug.notification.application.port.NotificationInboxViewReader`
- 수정 `org.codenbug.notification.domain.NotificationDomainService`
- 수정 `org.codenbug.notification.domain.entity.Notification`
- 유지 `org.codenbug.notification.domain.entity.NotificationContent`
- 유지 `org.codenbug.notification.domain.entity.UserId`
- 수정 `org.codenbug.notification.infra.NotificationStoreAdapter`
- 수정 `org.codenbug.notification.infra.NotificationRepository`
- 생성 또는 복구 `org.codenbug.notification.infra.NotificationInboxViewReaderAdapter`
- 유지 `org.codenbug.notification.ui.projection.NotificationListProjection`
- 제거 대상 `org.codenbug.notification.ui.repository.NotificationViewRepository`
- 제거 대상 `org.codenbug.notification.ui.repository.NotificationViewRepositoryImpl`
### 각 클래스의 layer와 책임
- `ui.NotificationQueryController`: 인증된 `userId` 추출, endpoint 보호, `Pageable` 최대 크기 제한, `sentAt DESC` 정규화만 담당한다.
- `application.NotificationQueryService`: inbox 목록, unread 목록, unread count, detail 조회 orchestration만 담당한다. list/count는 read-only, detail은 write transaction으로 둔다.
- `application.port.NotificationStore`: aggregate load/save, unread count, 기존 command path가 이미 쓰는 repository 추상화 유지.
- `application.port.NotificationInboxViewReader`: recipient-scoped list/unread 조회 전용 read port다. 저장, 삭제, 상태 변경 책임 금지.
- `domain.NotificationDomainService`: 생성 책임을 유지하면서 ownership 검증, unread 판정 같은 도메인 판단만 담당한다.
- `domain.entity.Notification`: 소유권 범위와 `UNREAD -> READ` 전이를 표현한다. non-owned 조회 실패 경로에서 변경되면 안 된다.
- `infra.NotificationStoreAdapter`: `NotificationRepository`를 통해 `NotificationStore` 계약을 충족한다.
- `infra.NotificationInboxViewReaderAdapter`: QueryDSL 또는 동등한 read adapter로 목록/unread projection 조회를 담당한다. side effect 금지.
- `ui.projection.NotificationListProjection`: 응답 projection 표현만 담당한다.
### 허용 의존성 방향
- `ui` -> `application`
- `application` -> `application.port`
- `application` -> `domain`
- `infra` -> `application.port`
- `infra` -> `domain`
- `domain` -> 상위 layer 금지
### 금지 import/framework dependency
- `application`, `domain`에서 `org.springframework.web.*`, `jakarta.servlet.*`, `org.codenbug.securityaop.aop.*`, `ResponseEntity`, `RsData` import 금지
- `application.NotificationQueryService`에서 `NotificationRepository` 직접 의존 금지
- `application.NotificationQueryService`에서 `ui.repository` package 직접 의존 금지
- read adapter에서 `save`, `delete`, transaction write 유발 로직 금지
- UC-030 구현을 위해 `RabbitTemplate`, consumer listener, Redis API, lock API 추가 금지
### bootstrap/configuration wiring
- 기존 Spring component scan과 constructor injection 유지
- 새 read port adapter는 `notification` 모듈 bean으로 등록
- controller는 `NotificationEmitterService` 의존을 유지하되 UC-030 경로와 분리

## 도메인 구현 계약
### Aggregate invariant
- 조회 결과는 항상 인증된 requester의 `userId` 범위여야 한다.
- inbox 목록은 항상 `sentAt` 최신순 페이지 결과여야 한다.
- unread 목록과 unread count는 `Notification` 상태를 바꾸면 안 된다.
- 성공한 detail 조회는 조회된 동일 root 한 건만 `READ` 상태로 전이할 수 있다.
- 미존재 알림 또는 타인 소유 알림 detail 요청은 aggregate 변경 없이 실패해야 한다.
### 상태 전이
- 생성 시 `isRead=false`
- detail 조회 성공 시 `isRead=false -> true`
- 이미 `READ`인 알림 재조회는 no-op
- 반복 detail 조회는 최종 상태 `READ`를 유지하는 idempotent 동작이어야 한다
### Entity/Value Object 생성 및 검증 규칙
- `UserId`: blank 금지, trim equality 유지
- `NotificationContent`: `title` 필수, `content` 필수, `targetUrl` optional 유지
- `Notification`: `userId`, `type`, `notificationContent` null 금지, `isRead` boolean storage 호환 유지
### Domain Service 여부와 책임
- 새 Domain Service 추가 불필요
- `NotificationDomainService`는 `createNotification(...)`, `createLegacyNotification(...)` 생성 계약 유지
- ownership 검증과 unread 판정 helper는 유지 또는 정리 가능하지만 조회 orchestration과 persistence 접근은 맡기지 않는다
### Domain Event 및 persistence compatibility
- UC-030에서 새 Domain Event 발행 금지
- `notification` 테이블의 `user_id`, `title`, `content`, `target_url`, `is_read`, `status`, `source_key` 호환성 유지
- `NotificationStatus` 의미와 listener 경로 계약 훼손 금지
### 다른 Aggregate/Bounded Context 협력 방식
- 인증/인가 판단은 gateway/security 경계 선행 책임이다
- UC-030은 인증된 requester ID만 입력으로 받고 외부 bounded context 호출을 추가하지 않는다
- `UC-031`, `UC-032`가 같은 aggregate를 공유하므로 `Notification`, `UserId`, `NotificationContent`, `isRead` 회귀 금지
### Transaction, idempotency, concurrency
- `getNotifications`, `getUnreadNotifications`, `getUnreadCount`는 `@Transactional(readOnly = true)`
- `getNotificationById`만 write transaction 허용
- 동시 detail 조회는 여러 요청이 성공해도 최종 상태가 `READ`면 허용
- 별도 lock, queue, retry 인프라 도입 금지

## 외부 계약 읽기 허용 목록
- 모듈 경계와 검증 명령 확인 -> `notification/AGENTS.md`
- required gate 확인 -> `.codex/test-gate.yaml`
- shared aggregate 정본 확인 -> `docs/changes/active/CHG-20260625-001.ddd-integration.md`
- 실제 패키지 taxonomy 확인 -> `notification/src/main/java/org/codenbug/notification/**`
- 실제 테스트 taxonomy 확인 -> `notification/src/test/java/org/codenbug/notification/**`
- 런타임 smoke 계약 확인이 필요할 때만 -> `scripts/run-app-infra.sh`
- 런타임 smoke 계약 확인이 필요할 때만 -> `scripts/check-app-infra.sh`
- 런타임 smoke 계약 확인이 필요할 때만 -> `scripts/run-app-server.sh`

## 작업 체크리스트
- [x] `notification/src/main/java/org/codenbug/notification/application/port/NotificationInboxViewReader.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapter.java`: recipient-scoped inbox/unread 조회를 `application` port + `infra` adapter로 분리하고 read side effect가 없음을 보장한다.
- [x] `notification/src/main/java/org/codenbug/notification/application/NotificationQueryService.java`: `NotificationViewRepository` 직접 의존을 제거하고 `NotificationInboxViewReader` + `NotificationStore` + `NotificationDomainService` 조합으로 list, unread, count, detail 흐름을 재구성한다.
- [x] `notification/src/main/java/org/codenbug/notification/ui/NotificationQueryController.java`: `Role.USER` 보호를 유지하고 authenticated `userId` 전달, page size 최대 100 clamp, `sentAt DESC` 정규화를 모든 inbox/unread 경로에 일관 적용한다.
- [x] `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`, `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`: 타인 소유 거절, unread 판정, read 전이 idempotency를 명확히 하고 UC-032 생성 계약 회귀가 없게 유지한다.
- [x] `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`: unread count와 aggregate load/save 계약이 그대로 작동하도록 정리하고 query 책임을 read adapter 쪽으로 분리한다.
- [x] `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepository.java`, `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepositoryImpl.java`: 남은 참조가 0건이면 삭제를 완료하고, 참조가 남아 있으면 executor가 그 참조를 먼저 제거한 뒤 삭제한다.
- [x] `notification/src/test/java/org/codenbug/notification/application/NotificationQueryServiceTest.java`: 목록/unread는 no-save, unread count는 no-save, 미존재 detail 거절, 타인 소유 detail 거절, unread detail save 1회, already-read no-op, repeated detail idempotency를 검증한다.
- [x] `notification/src/test/java/org/codenbug/notification/ui/NotificationQueryControllerTest.java`: unauthenticated/unauthorized 차단, authenticated `userId` 전달, size clamp, latest-first sort normalization을 검증한다.
- [x] `notification/src/test/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapterTest.java`: cross-recipient no-leak, unread filter, latest-first pagination을 검증하는 focused infra 테스트를 추가한다.
- [x] `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`: application layer가 `ui.repository`가 아니라 `application.port`에만 의존하는 계약을 검증한다.
- [x] `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`: ownership 검증, unread 판정, idempotent read transition, `createNotification(...)`, `createLegacyNotification(...)` 회귀를 검증한다.
- [x] `notification/src/test/java/org/codenbug/notification/infra/event/PurchaseEventListenerTest.java`, `notification/src/test/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListenerTest.java`: shared aggregate 변경으로 listener 경로가 깨지지 않는지 최소 회귀를 확인한다.
- [x] `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`: UC-030 때문에 런타임 기동 계약이 깨진 경우에만 최소 수정하고, 필요 없으면 무수정으로 남긴다.

## 집중 검증
- [x] Build: `./gradlew --no-daemon --console=plain :notification:build` -> `notification` 모듈 compile, test, packaging 성공
- [x] Focused tests: `./gradlew --no-daemon :notification:test --tests '*NotificationQueryServiceTest' --tests '*NotificationQueryControllerTest' --tests '*NotificationInboxViewReaderAdapterTest' --tests '*NotificationApplicationServicePortTest' --tests '*NotificationDomainServiceTest'` -> UC-030 핵심 규칙 통과
- [x] Architecture test: `./gradlew --no-daemon --console=plain architectureRules` -> 계층/의존성 위반 0건
- [x] E2E 또는 maintenance verification: `./gradlew --no-daemon :notification:test` -> UC-030 및 shared aggregate 회귀 포함 전체 `notification` 테스트 통과
- [x] Test gate: `N/A - .codex/test-gate.yaml`의 `required`가 비어 있어 추가 required stage 없음
- [x] Runtime server verification: `./harness run app --timeout 120` -> app infra/server tmux 기동 성공, eureka/app/gateway health 확인 완료
- [x] Static analysis: `./gradlew --no-daemon --console=plain :notification:compileJava :notification:compileTestJava` 및 `architectureRules` -> import/package 오류와 계층 위반 0건
### 중단 조건
- `notification` 밖 파일 수정이 필요해지는 경우
- `NotificationContent.content` 필수 규칙이나 `isRead` 저장 호환성을 깨야만 통과 가능한 경우
- detail 실패 경로에서 상태 불변을 보장할 수 없는 구조적 결함이 발견되는 경우
- 런타임 범위 정리 전에 `.git/index.lock` 또는 대량 generated file 오염이 계속 남아 scope recovery를 막는 경우

## 9. OWASP Security Review
- pending `security_plan_reviewer`; attack surface: `GET /api/v1/notifications`, `GET /api/v1/notifications/unread`, `GET /api/v1/notifications/count/unread`, `GET /api/v1/notifications/{id}`
- BOLA 방어: detail 조회는 controller 인증 + service ownership 검증 둘 다 통과해야 한다.
- Broken Access Control 방어: 모든 UC-030 endpoint는 `Role.USER` 보호를 유지한다.
- Resource consumption 방어: inbox/unread pageable size는 100 이하로 clamp한다.
- Data exposure 방어: read adapter는 recipient-scoped filter를 강제하고 cross-recipient leak 테스트를 둔다.
- State integrity 방어: list/unread/count 경로는 save 금지, detail success 경로만 단건 save 허용.

## 10. 완료 조건
- 모든 체크박스가 `- [x]`.
- 필요한 테스트가 존재하고 통과.
- Build, focused tests, architecture test, E2E 또는 maintenance verification, Test gate, Runtime server verification, Static analysis 결과가 계획에 기록된다.
- active -> completed 전이는 `complete-work-item-plan`만 수행한다.

## 11. 검증 결과
- Build: PASS - `./gradlew --no-daemon --console=plain :notification:build`
- Focused tests: PASS - `./gradlew --no-daemon --console=plain :notification:test --tests '*NotificationQueryServiceTest' --tests '*NotificationQueryControllerTest' --tests '*NotificationInboxViewReaderAdapterTest' --tests '*NotificationApplicationServicePortTest' --tests '*NotificationDomainServiceTest'`
- Architecture test: PASS - `./gradlew --no-daemon --console=plain architectureRules`
- E2E 또는 maintenance verification: PASS - `./gradlew --no-daemon --console=plain :notification:test`
- Test gate: PASS - `.codex/test-gate.yaml` required stage 없음
- Runtime server verification: PASS - `./harness run app --timeout 120`
- Static analysis: PASS - `:notification:compileJava`, `:notification:compileTestJava`, `architectureRules`
