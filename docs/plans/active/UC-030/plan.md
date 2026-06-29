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
- 목표: 인증된 `Notification Recipient`가 gateway 경유로 자기 소유 알림의 inbox 목록, unread 목록, unread count, detail 조회를 수행하고, 성공한 detail 조회에서 동일 알림 1건만 `UNREAD -> READ` 전이됨을 관찰 가능하게 만든다.

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
  - `notification/src/main/java/org/codenbug/notification/infra/event/PurchaseEventListener.java`
  - `notification/src/main/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListener.java`
  - `notification/src/main/java/org/codenbug/notification/ui/projection/NotificationListProjection.java`
  - `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepository.java`
  - `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepositoryImpl.java`
  - `notification/build.gradle`
  - `notification/src/test/java/org/codenbug/notification/application/service/NotificationApplicationServicePortTest.java`
  - `notification/src/test/java/org/codenbug/notification/domain/service/NotificationDomainServiceTest.java`
  - `notification/src/test/java/org/codenbug/notification/**`
  - `scripts/run-app-infra.sh`
  - `scripts/check-app-infra.sh`
  - `scripts/run-app-server.sh`
### 경계 해석 규칙
  - `docs/use-cases/UC-030/affected-files.md`의 `controller/application/service/domain/service/infrastructure` 표기는 broad inventory로만 읽고, 실제 executor 계약은 현재 repo taxonomy인 `ui/application/domain/infra`를 따른다.
  - `PurchaseEventListener`, `PurchaseNotificationEventListener`, `notification/build.gradle`은 compile/runtime 계약 확인용 영향 파일이다. UC-030 구현은 원칙적으로 query/read-transition 경로에 집중하고, 이 파일들은 빌드/런타임 검증이 깨질 때만 최소 수정한다.

## 패키지 및 의존성 계약
### 생성/수정 클래스와 정확한 package
  - `org.codenbug.notification.ui.NotificationQueryController`
  - `org.codenbug.notification.application.NotificationQueryService`
  - `org.codenbug.notification.application.port.NotificationStore`
  - `org.codenbug.notification.domain.entity.Notification`
  - `org.codenbug.notification.domain.entity.UserId`
  - `org.codenbug.notification.domain.entity.NotificationContent`
  - `org.codenbug.notification.domain.NotificationDomainService`
  - `org.codenbug.notification.infra.NotificationStoreAdapter`
  - `org.codenbug.notification.infra.NotificationRepository`
  - `org.codenbug.notification.ui.projection.NotificationListProjection`
  - `org.codenbug.notification.ui.repository.NotificationViewRepository`
  - `org.codenbug.notification.ui.repository.NotificationViewRepositoryImpl`
### 각 클래스의 layer와 책임
  - `ui.NotificationQueryController`: `@AuthNeeded`, `@RoleRequired({Role.USER})`, `LoggedInUserContext` 사용. `/api/v1/notifications`, `/unread`, `/count/unread`, `/{id}`만 UC-030 범위에서 조율. `Pageable` 최대 size 100, latest-first 정규화 수행. `/subscribe` behavior 비변경.
  - `application.NotificationQueryService`: inbox/unread/count/detail 유스케이스 조합. 웹 타입 차단. detail read transition의 트랜잭션 경계 보유.
  - `application.port.NotificationStore`: aggregate load/save/unread count application port.
  - `domain.entity.Notification`: recipient ownership, unread/read 상태, `isUnread`, `markAsRead` 보유.
  - `domain.entity.UserId`: null/blank 금지, trim 기반 equality 유지.
  - `domain.entity.NotificationContent`: `title` 필수, 길이 검증 유지, `content` required 의미 유지, `targetUrl` optional 유지.
  - `domain.NotificationDomainService`: ownership 검증, `canMarkAsRead` 판단만 담당. 조회 orchestration, repository access, transaction 제어 금지.
  - `infra.NotificationStoreAdapter`: `NotificationStore`를 Spring Data JPA로 위임.
  - `infra.NotificationRepository`: `Notification` persistence/query method 선언. application layer 직접 주입 금지.
  - `ui.projection.NotificationListProjection`: 목록 응답 projection. aggregate mutation 금지.
  - `ui.repository.NotificationViewRepository`: projection read port. side effect 금지.
  - `ui.repository.NotificationViewRepositoryImpl`: QueryDSL로 recipient scope, unread filter, `sentAt DESC`, pagination 구현.
### 허용 의존성 방향
  - `ui -> application`
  - `application -> application.port`
  - `application -> domain`
  - `application -> ui.repository`
  - `infra -> application.port`
  - `infra -> domain`
  - `ui.repository -> domain Q-type/projection`
  - `domain`은 상위 layer 의존 금지
### 금지 import/framework dependency
  - `application`/`domain`에서 `jakarta.servlet.*`, `org.springframework.web.*`, `org.codenbug.securityaop.aop.*`, `ResponseEntity`, `RsData` import 금지
  - `application`에서 `NotificationRepository` 직접 주입 금지
  - `ui.repository`에서 `save`, `delete`, `@Transactional` write 로직 금지
  - UC-030 구현을 위해 `RedisTemplate`, messaging listener, AMQP consumer, lock API 추가 금지
### bootstrap/configuration wiring
  - 기존 component scan, constructor injection 유지
  - 새 bean 필요 시 `notification` 모듈 내부에서만 해결
  - `NotificationQueryController`의 `NotificationEmitterService` wiring 유지. UC-030 작업으로 behavior 변경 금지

## 도메인 구현 계약
### Aggregate invariant
  - 모든 조회 결과는 인증된 requester의 `userId` 범위 안에서만 노출된다.
  - inbox는 저장 aggregate가 아니라 `Notification` 집합에서 파생된 query view다.
  - unread 목록, unread count는 상태를 바꾸지 않는다.
  - 성공한 detail 조회는 조회된 동일 `Notification` 한 건만 `UNREAD -> READ` 전이 가능하다.
  - non-existent 또는 foreign-owned detail 요청은 aggregate 변경 없이 실패한다.
### 상태 전이
  - 생성 직후 `isRead=false`
  - `getNotificationById` 성공 경로에서만 `isRead=false -> true`
  - 이미 `READ`인 알림 재조회는 no-op
  - repeated detail 조회는 idempotent해야 하며 타 알림 state를 건드리면 안 된다
### Entity/Value Object 생성 및 검증 규칙
  - `UserId`: null/blank 금지, trim 유지
  - `NotificationContent`: `title` 필수, 100자 초과 금지, `content` required 의미 유지, `targetUrl`만 optional
  - `Notification`: `userId`, `type`, `notificationContent` null 금지, 기존 JPA field/column 호환 유지
  - boolean `isRead` persisted 표현 유지, canonical 의미는 `UNREAD/READ`
### Domain Service 여부와 책임
  - 새 Domain Service 추가 불필요
  - 기존 `NotificationDomainService`는 `validateUserOwnership`, `canMarkAsRead`만 맡는다
  - ownership 판단은 `markAsRead` 이전에 반드시 수행
### Domain Event 및 persistence compatibility
  - UC-030에서 새 Domain Event 발행 금지
  - `notification` 테이블 스키마, `user_id`, `content`, `target_url`, `is_read`, `status`, `source_key` 호환성 유지
  - `NotificationRepository`의 기존 삭제/상태 갱신 메서드 의미 변경 금지
### 다른 Aggregate/Bounded Context 협력 방식
  - 인증과 role 판정은 gateway/security stack 선행 책임
  - UC-030 구현은 인증된 `userId` 문자열만 입력으로 받고 다른 BC API 호출 추가 금지
  - `UC-031`, `UC-032`도 동일 `NotificationAggregate`를 공유하므로 `Notification`, `UserId`, `NotificationContent`, `isRead` semantics 회귀 방지 필요
### Transaction, idempotency, concurrency
  - `getNotifications`, `getUnreadNotifications`, `getUnreadCount`는 `@Transactional(readOnly = true)` 유지
  - `getNotificationById`만 write transaction
  - concurrent detail 조회는 최종 상태가 `READ`면 성공으로 간주
  - lock/outbox/optimistic version 도입 금지

## 외부 계약 읽기 허용 목록
- module taxonomy와 verification command 확인 -> `notification/AGENTS.md`
- required test gate 확인 -> `.codex/test-gate.yaml`
- runtime smoke 계약 확인 -> `scripts/run-app-infra.sh`
- runtime smoke 계약 확인 -> `scripts/check-app-infra.sh`
- runtime smoke 계약 확인 -> `scripts/run-app-server.sh`
- shared aggregate 호환성 확인 -> `docs/changes/active/CHG-20260625-001.ddd-integration.md`
- shared aggregate 기계 판독 계약 확인 -> `docs/changes/active/CHG-20260625-001.ddd-integration.json`
- broad inventory와 실제 경로 taxonomy 충돌 해소 -> `docs/use-cases/UC-030/affected-files.md`

## 작업 체크리스트
- [ ] `notification/src/main/java/org/codenbug/notification/ui/NotificationQueryController.java`: UC-030 대상 endpoint 4개가 모두 `Role.USER` 보호, authenticated `userId` 전달, `sentAt DESC` 유지, `size` 최대 100 정규화, `/subscribe` ADMIN 경로 비변경 규칙을 만족하도록 정리한다.
- [ ] `notification/src/main/java/org/codenbug/notification/application/NotificationQueryService.java`: list/unread/count는 무상태 조회, detail은 `findById -> ownership 검증 -> unread일 때만 mark/save -> dto 반환` 순서를 강제하고 write transaction을 detail에만 적용한다.
- [ ] `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`: ownership 거절과 read 가능 여부 판단이 aggregate invariant를 직접 증명하고 조회 orchestration을 추가하지 않게 맞춘다.
- [ ] `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`: `isUnread`, `markAsRead`, no-op 재조회 규칙이 canonical `UNREAD/READ` 의미와 idempotent read transition을 유지하게 정리한다.
- [ ] `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`: blank 금지, trim equality 유지로 recipient scope 비교 규칙을 보장한다.
- [ ] `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`: 제목/내용 검증을 shared aggregate 계약과 호환되게 유지한다.
- [ ] `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`: application이 port에만 의존하고 unread count, aggregate load/save contract를 인프라가 충족하도록 맞춘다.
- [ ] `notification/src/main/java/org/codenbug/notification/ui/projection/NotificationListProjection.java`, `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepository.java`, `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepositoryImpl.java`: recipient scope, unread filter, latest-first pagination, no-write projection 규칙을 충족한다.
- [ ] `notification/src/test/java/org/codenbug/notification/application/service/NotificationApplicationServicePortTest.java`: application layer가 `NotificationStore` port에만 의존함을 검증한다.
- [ ] `notification/src/test/java/org/codenbug/notification/domain/service/NotificationDomainServiceTest.java`: ownership 검증, unread 판단, read transition idempotency 규칙을 검증한다.
- [ ] `notification/src/test/java/org/codenbug/notification/**`: controller/service/repository 테스트를 추가 또는 보강해 unauthenticated 거절, foreign-owned/non-existent detail 거절, unread count no-save, repeated detail idempotency, cross-recipient no-leak, latest-first pagination을 검증한다.
- [ ] `notification/src/main/java/org/codenbug/notification/infra/event/PurchaseEventListener.java`, `notification/src/main/java/org/codenbug/notification/infra/messaging/PurchaseNotificationEventListener.java`, `notification/build.gradle`: UC-030 변경 후 compile/runtime 계약이 깨질 때만 최소 수정하고, 그대로면 무변경으로 유지한다.
- [ ] `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`: 실제 notification 변경으로 앱 기동 계약이 깨질 때만 최소 수정한다. 변경 없으면 untouched로 둔다.

## 집중 검증
- [ ] Build: `./gradlew --no-daemon :notification:build` -> `notification` 모듈 compile, test, QueryDSL generation 성공
- [ ] Focused tests: `./gradlew --no-daemon :notification:test --tests '*NotificationQueryControllerTest' --tests '*NotificationQueryServiceTest' --tests '*NotificationViewRepositoryImplTest' --tests '*NotificationDomainServiceTest' --tests '*NotificationApplicationServicePortTest'` -> recipient scope, unread filter, unread count no-save, ownership rejection, detail read transition, port boundary 모두 통과
- [ ] Architecture test: `./gradlew --no-daemon architectureRules && semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java` -> 계층/패키지 경계 위반 없음
- [ ] E2E 또는 maintenance verification: `./gradlew --no-daemon :notification:test` -> UC-030 계약 및 shared aggregate 회귀 포함 전체 `notification` 테스트 통과
- [ ] Test gate: `.codex/test-gate.yaml` required stage PASS
- [ ] Runtime server verification: `./harness run app --timeout 120` -> infra/eureka/app/gateway health 성공 후 gateway를 통해 inbox, unread, count, detail smoke 확인
- [ ] Static analysis: `./gradlew --no-daemon :notification:compileTestJava` -> test 포함 컴파일 성공, package/import 계약 위반 부재 확인
### 중단 조건
  - `notification` 외 경로 수정이 필요해진 경우
  - `NotificationContent.content` nullability 또는 `isRead` persisted 표현을 바꿔야만 계약을 만족하는 경우
  - detail 실패 경로에서 save/no-leak를 보장할 수 없는 구조적 결함이 발견된 경우
  - runtime smoke에 Docker/infra 미가용 등 환경 차단이 발생한 경우

## 9. OWASP Security Review
- pending `security_plan_reviewer`; attack surface:
  - BOLA/API1: `GET /api/v1/notifications/{id}`의 foreign-owned detail 접근
  - Broken Access Control: inbox/unread/count/detail endpoint의 인증 또는 `Role.USER` 누락
  - Unrestricted Resource Consumption/API4: 과대 `size` 값으로 인한 과다 조회
  - Data Exposure: cross-recipient list/unread/count/detail projection 누출
  - State Integrity: denied path 또는 repeated detail path에서 잘못된 save 호출
- 구현 작업:
  - controller에서 endpoint 4개 모두 인증/권한 annotation과 authenticated requester ID 주입을 고정한다.
  - service/domain에서 ownership 검증을 `markAsRead`보다 먼저 강제한다.
  - controller에서 page size 상한 100과 latest-first 정렬을 강제한다.
  - unread list/count 경로와 실패 경로에서 save 호출이 없음을 보장한다.
  - detail 성공 경로에서 조회 대상 1건만 `READ`로 바뀌고 다른 recipient 데이터가 노출되지 않게 보장한다.
- 보안 테스트:
  - unauthenticated 요청 거절 controller 테스트
  - foreign-owned detail 거절 및 no-save service 테스트
  - non-existent detail 거절 및 no-save service 테스트
  - unread count/list no-state-change 테스트
  - repeated detail idempotency와 cross-recipient no-leak 테스트
  - page size 상한과 latest-first pagination 테스트
- 승인 기준:
  - endpoint 4개 모두 인증/권한 누락 없음
  - detail 실패 경로는 aggregate/state 변화 없음
  - 성공한 detail 조회는 단 1건만 `READ`
  - list/unread/count/detail 어디에서도 타 recipient 데이터 미노출

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
- Test gate: pending
- Runtime server verification: pending
- Static analysis: pending
