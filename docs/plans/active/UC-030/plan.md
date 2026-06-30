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
- 목표: 인증된 사용자가 자신의 `Recipient User ID` 범위 안에서 알림 inbox 목록, unread 목록, unread count, detail을 조회하고, detail 성공 시 해당 `Notification` 1건만 `UNREAD -> READ`로 전이되게 한다.

## 2. 구현하지 말아야 할 것
- `notification` 모듈 밖 비즈니스 코드 수정
- `app/`, `platform/`에 알림 조회 규칙 추가
- `application`, `domain` 계층에 servlet, controller, `LoggedInUserContext`, `RsData`, `ResponseEntity` 전달
- Redis, outbox, queue, distributed lock, cache 같은 새 인프라 추가
- `Notification` 저장 shape의 호환성 파괴
- 기존 SSE 구독 경로와 생성/삭제 유스케이스 계약 변경
- `application-secret.yml` 조회, 출력, 수정

## 실행 경계
- 대상 bounded context/module: `notification`
- 대상 aggregate root: `org.codenbug.notification.domain.entity.Notification`
- 범위 판정 기준: `docs/use-cases/UC-030/affected-files.md`의 legacy taxonomy는 현재 repo 실경로에 다음처럼 대응시킨다. `controller` -> `ui`, `application/service` -> `application`, `domain/service` -> `domain`, `infrastructure` -> `infra`.
- scope repair: 현재 work item은 `notification/**` 수정과 notification-local 검증만 허용한다. `scripts/run-app-*.sh` 실행과 repo root `architectureRules` 재실행은 `app/build/**`, `event/build/**`, `platform/gateway/build/**` 같은 범위 밖 산출물을 만들 수 있으므로 완료 게이트에서 제외한다.
### 수정 허용 경로
- `notification/src/main/java/org/codenbug/notification/ui/**`
- `notification/src/main/java/org/codenbug/notification/application/**`
- `notification/src/main/java/org/codenbug/notification/domain/**`
- `notification/src/main/java/org/codenbug/notification/infra/**`
- `notification/src/test/java/org/codenbug/notification/**`
- 필요 시 `notification/build.gradle`
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
- `notification/src/main/java/org/codenbug/notification/application/port/NotificationInboxViewReader.java`
- `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapter.java`
- `notification/src/main/java/org/codenbug/notification/ui/projection/NotificationListProjection.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationQueryServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/ui/NotificationQueryControllerTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapterTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/event/PurchaseEventListenerTest.java`
- `notification/src/test/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListenerTest.java`

## 패키지 및 의존성 계약
### 생성/수정 클래스와 정확한 package
- 수정 `org.codenbug.notification.ui.NotificationQueryController`
- 수정 `org.codenbug.notification.application.NotificationQueryService`
- 수정 `org.codenbug.notification.application.port.NotificationStore`
- 수정 `org.codenbug.notification.application.port.NotificationInboxViewReader`
- 수정 `org.codenbug.notification.domain.NotificationDomainService`
- 수정 `org.codenbug.notification.domain.entity.Notification`
- 유지 `org.codenbug.notification.domain.entity.NotificationContent`
- 유지 `org.codenbug.notification.domain.entity.UserId`
- 수정 `org.codenbug.notification.infra.NotificationStoreAdapter`
- 수정 `org.codenbug.notification.infra.NotificationRepository`
- 수정 `org.codenbug.notification.infra.NotificationInboxViewReaderAdapter`
- 유지 `org.codenbug.notification.ui.projection.NotificationListProjection`
### 각 클래스의 layer와 책임
- `ui.NotificationQueryController`: 인증/권한 annotation 유지, authenticated `userId` 추출, pageable size 상한, `sentAt DESC` 정규화만 담당한다.
- `application.NotificationQueryService`: inbox 목록, unread 목록, unread count, detail 조회 orchestration만 담당한다.
- `application.port.NotificationStore`: aggregate load/save, unread count 조회 계약만 노출한다.
- `application.port.NotificationInboxViewReader`: recipient-scoped 목록과 unread 목록 read-model 조회만 담당한다.
- `domain.NotificationDomainService`: 생성 계약 유지, ownership 검증과 unread/read 판단에 필요한 도메인 헬퍼만 담당한다.
- `domain.entity.Notification`: 소유권, unread/read 전이, idempotent read marking 규칙을 보유한다.
- `infra.NotificationStoreAdapter`: `NotificationRepository`를 사용해 `NotificationStore` 계약을 충족한다.
- `infra.NotificationInboxViewReaderAdapter`: recipient 필터, unread 필터, latest-first pagination을 side effect 없이 수행한다.
### 허용 의존성 방향
- `ui` -> `application`
- `application` -> `application.port`
- `application` -> `domain`
- `infra` -> `application.port`
- `infra` -> `domain`
- `domain` -> 상위 계층 의존 금지
### 금지 import/framework dependency
- `application`, `domain`에서 `org.springframework.web.*`, `jakarta.servlet.*`, `org.codenbug.securityaop.aop.*`, `ResponseEntity`, `RsData` import 금지
- `application.NotificationQueryService`에서 `NotificationRepository` 직접 의존 금지
- `application.NotificationQueryService`에서 `ui` 하위 repository/query 구현 직접 의존 금지
- read adapter에서 `save`, `delete`, write transaction 유발 로직 금지
- UC-030 구현 범위에 `RabbitTemplate`, listener, Redis API, lock API 추가 금지
### bootstrap/configuration wiring
- 기존 Spring component scan, constructor injection, bean 등록 방식 유지
- 새 wiring이 필요하면 `notification` 모듈 내부 component만 사용한다
- controller는 query path에서 emitter, messaging, command flow와 결합하지 않는다

## 도메인 구현 계약
### Aggregate invariant
- 모든 조회 결과는 인증된 요청자의 `Recipient User ID` 범위로만 제한된다.
- inbox 목록은 항상 latest-first paginated 결과여야 한다.
- unread 목록과 unread count 조회는 상태를 변경하지 않는다.
- 성공한 detail 조회는 반환된 동일 `Notification` 1건만 `UNREAD -> READ`로 전이할 수 있다.
- 미존재 또는 타인 소유 detail 조회는 aggregate 변경 없이 실패해야 한다.
### 상태 전이
- 생성 직후 상태는 `UNREAD`다.
- detail 조회 성공 시에만 `UNREAD -> READ` 전이가 허용된다.
- 이미 `READ`인 알림 재조회는 no-op이어야 한다.
- 반복 detail 조회 후 최종 상태는 항상 `READ`여야 한다.
### Entity/Value Object 생성 및 검증 규칙
- `UserId`: blank 금지, trim equality 유지
- `NotificationContent`: `title` required, `content` required, `targetUrl` optional 유지
- `Notification`: `userId`, `type`, `notificationContent`, `sentAt` null 금지, persisted `isRead` 호환 유지
### Domain Service 여부와 책임
- 새 Domain Service 추가 금지
- `NotificationDomainService`는 `createNotification(...)`, `createLegacyNotification(...)` 계약을 유지한다
- ownership 검증과 read-state helper는 둘 수 있지만 persistence 접근과 query orchestration은 맡기지 않는다
### Domain Event 및 persistence compatibility
- UC-030 범위에서 새 Domain Event 발행 금지
- `notification` 테이블의 `user_id`, `title`, `content`, `target_url`, `is_read`, `status`, `source_key` 호환성 유지
- baseline `NotificationStatus` 의미를 깨지 않는다
### 다른 Aggregate/Bounded Context 협력 방식
- 인증 판단은 gateway/security 경계 선행 책임으로 둔다
- UC-030은 외부 bounded context 호출을 추가하지 않는다
- `UC-031`, `UC-032`와 shared aggregate를 쓰므로 `Notification`, `UserId`, `NotificationContent`, read-state 회귀 금지
### Transaction, idempotency, concurrency
- `getNotifications`, `getUnreadNotifications`, `getUnreadCount`는 `@Transactional(readOnly = true)`를 유지한다
- `getNotificationById`만 write transaction 허용
- 동시 detail 조회는 여러 요청이 성공해도 최종 상태가 `READ`면 허용한다
- 별도 lock, queue, retry 정책 추가 금지

## 외부 계약 읽기 허용 목록
- 모듈 경계와 검증 명령 확인 -> `notification/AGENTS.md`
- required gate 확인 -> `.codex/test-gate.yaml`
- shared aggregate 정본 확인 -> `docs/changes/active/CHG-20260625-001.ddd-integration.md`
- 유스케이스별 기대 동작 확인 -> `docs/use-cases/UC-030/use-case.md`
- 이벤트 흐름과 invariant 확인 -> `docs/use-cases/UC-030/event-storming.md`
- 승인된 기술 결정 확인 -> `docs/use-cases/UC-030/technical-decisions.md`
- 실제 패키지 taxonomy 확인 -> `notification/src/main/java/org/codenbug/notification/**`
- 실제 테스트 taxonomy 확인 -> `notification/src/test/java/org/codenbug/notification/**`
- 런타임 smoke 계약 확인이 필요할 때만 -> `scripts/run-app-infra.sh`
- 런타임 smoke 계약 확인이 필요할 때만 -> `scripts/check-app-infra.sh`
- 런타임 smoke 계약 확인이 필요할 때만 -> `scripts/run-app-server.sh`

## 작업 체크리스트
- [ ] TASK-001 `notification/src/main/java/org/codenbug/notification/application/NotificationQueryService.java`: `NotificationStore`와 `NotificationInboxViewReader`만 사용해 목록, unread 목록, unread count, detail 흐름을 구성하고, detail 성공 경로만 단건 저장하게 만든다.
- [ ] TASK-002 `notification/src/main/java/org/codenbug/notification/ui/NotificationQueryController.java`: `Role.USER` 보호, authenticated `userId` 전달, size clamp, `sentAt DESC` 정규화를 inbox/unread 경로에 일관 적용한다.
- [ ] TASK-003 `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`, `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`: 타인 소유 거절, unread 판정, idempotent read transition을 명확히 하고 shared aggregate 생성 계약을 깨지 않게 유지한다.
- [ ] TASK-004 `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`: unread count와 aggregate load/save 계약을 유지하고 조회 책임이 command adapter로 새지 않게 정리한다.
- [ ] TASK-005 `notification/src/main/java/org/codenbug/notification/application/port/NotificationInboxViewReader.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapter.java`: recipient-scoped 목록, unread 필터, latest-first pagination을 side effect 없이 읽는 read adapter 계약을 고정한다.
- [ ] TEST-001 `notification/src/test/java/org/codenbug/notification/application/NotificationQueryServiceTest.java`: 목록/unread/count는 상태를 바꾸지 않고, missing/foreign-owned detail은 저장하지 않으며, unread detail만 1회 저장되고 repeated detail이 idempotent함을 검증한다.
- [ ] TEST-002 `notification/src/test/java/org/codenbug/notification/ui/NotificationQueryControllerTest.java`: unauthenticated/unauthorized 차단, authenticated `userId` 전달, page size clamp, latest-first sort normalization을 검증한다.
- [ ] TEST-003 `notification/src/test/java/org/codenbug/notification/infra/NotificationInboxViewReaderAdapterTest.java`: cross-recipient no-leak, unread filter, latest-first pagination을 검증한다.
- [ ] TEST-004 `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`: application layer가 `NotificationRepository`나 `ui` query 구현이 아니라 `application.port`에만 의존함을 검증한다.
- [ ] TEST-005 `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`: ownership helper, unread/read 전이, `createNotification(...)`, `createLegacyNotification(...)` 회귀를 검증한다.
- [ ] TEST-006 `notification/src/test/java/org/codenbug/notification/infra/event/PurchaseEventListenerTest.java`, `notification/src/test/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListenerTest.java`: shared aggregate 변경이 listener 경로에 회귀를 만들지 않는 최소 범위 검증을 추가한다.

## 집중 검증
- scope repair 이후 executor는 notification-local 명령만 사용한다. 범위 밖 build artifact를 유발하는 런타임/ArchUnit 명령은 완료 조건에서 제외하고, architecture evidence는 notification 대상 Semgrep 결과로 대체한다.
- [ ] VERIFY-001 Build: `./gradlew --no-daemon --console=plain :notification:build` -> `notification` 모듈 compile, test, packaging 성공
- [ ] VERIFY-002 Focused tests: `./gradlew --no-daemon --console=plain :notification:test --tests '*NotificationQueryServiceTest' --tests '*NotificationQueryControllerTest' --tests '*NotificationInboxViewReaderAdapterTest' --tests '*NotificationApplicationServicePortTest' --tests '*NotificationDomainServiceTest'` -> UC-030 핵심 규칙 통과
- [ ] VERIFY-003 Architecture test: N/A - repo root `./gradlew --no-daemon --console=plain architectureRules`는 `:app:architectureRules`로 위임돼 범위 밖 산출물을 만들 수 있으므로 재실행하지 않는다. 현재 architecture evidence는 `VERIFY-007`의 `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java` 결과로 대체하며 기대 결과는 notification 범위 blocking finding 0건이다.
- [ ] VERIFY-004 E2E 또는 maintenance verification: `./gradlew --no-daemon --console=plain :notification:test` -> UC-030 및 shared aggregate 회귀 포함 전체 `notification` 테스트 통과
- [ ] VERIFY-005 Test gate: `.codex/test-gate.yaml`의 `required: []` 확인 -> 추가 강제 stage 없이 현재 검증 묶음으로 gate 충족 기록
- [ ] VERIFY-006 Runtime server verification: N/A - `python3 -m harness_codex run app --timeout 120`, `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh` 재실행은 gateway/build resource mirror를 생성해 current execution boundary를 벗어나므로 완료 게이트에서 제외한다.
- [ ] VERIFY-007 Static analysis: `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java` -> blocking architecture finding 0건
### 중단 조건
- `notification` 밖 코드 수정이 필수로 드러나는 경우
- `NotificationContent.content` required 규칙 또는 persisted `isRead` 호환성을 깨야만 구현 가능한 경우
- detail 실패 경로에서 상태 불변을 보장할 수 없는 구조적 결함이 발견되는 경우
- active worktree 변경과 충돌해 현재 범위 안에서 안전한 구현 계획 유지가 불가능한 경우

## 9. OWASP Security Review
- pending `security_plan_reviewer`; attack surface: `GET /api/v1/notifications`, `GET /api/v1/notifications/unread`, `GET /api/v1/notifications/count/unread`, `GET /api/v1/notifications/{id}`
- BOLA 방어: detail 조회는 controller 인증/권한과 service ownership 검증을 둘 다 통과해야 한다.
- Broken Access Control 방어: 모든 UC-030 endpoint는 authenticated `Role.USER` 경로로만 노출한다.
- Resource consumption 방어: pageable size는 상한을 강제한다.
- Data exposure 방어: read adapter는 recipient filter를 강제하고 cross-recipient leak 테스트를 둔다.
- State integrity 방어: 목록/unread/count 경로는 저장 금지, detail 성공 경로만 단건 저장 허용

## 10. 완료 조건
- 모든 체크박스가 `- [x]`.
- 필요한 테스트가 존재하고 통과.
- Build, focused tests, E2E 또는 maintenance verification, Test gate, Static analysis 결과와 architecture/runtime verification 제외 근거가 기록된다.
- active -> completed 전이는 `complete-work-item-plan`만 수행한다.

## 10. 검증 결과
- Build: pending
- Tests: pending
- E2E 또는 maintenance verification: pending
- Test gate: pending
- Runtime server verification: pending
- Static analysis: pending
