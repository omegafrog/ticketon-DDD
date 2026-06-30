---
change_set_id: CHG-20260625-001
contract_version: 1
doc_id: "UC-031:plan"
doc_type: plan
source_docs:
  - docs/changes/active/CHG-20260625-001.md
  - docs/use-cases/UC-031/use-case.md
  - docs/use-cases/UC-031/event-storming.md
  - docs/use-cases/UC-031/ddd-design.md
  - docs/changes/active/CHG-20260625-001.ddd-integration.md
  - docs/changes/active/CHG-20260625-001.ddd-integration.json
  - docs/use-cases/UC-031/technical-decisions.md
  - docs/use-cases/UC-031/affected-files.md
  - docs/use-cases/UC-031/e2e-goal.md
  - ARCHITECTURE.md
  - .codex/repository-settings.md
status: active
work_item_id: UC-031
---
# 구현 계획

## 1. 구현 목표
- ChangeSet: `CHG-20260625-001`
- Work item: `UC-031`
- 목표: 인증된 `Notification Recipient`가 gateway 경유 삭제 API로 자신의 `Notification`을 단건, `selected-set`, `all-owned` 범위로 삭제하고, `selected-set`에 existing foreign-owned `Notification`이 하나라도 포함되면 전체 거절되며 이미 없는 owned `Notification`은 무시되도록 구현한다.

## 2. 구현하지 말아야 할 것
- `platform/gateway` 진입 구조, `app/` orchestration-only 규칙, `application` 계층의 web type 금지 규칙을 바꾸지 않는다.
- soft delete 상태, `Deleted` persistence state, retry, circuit breaker, outbox/inbox, cache, distributed lock을 추가하지 않는다.
- `UC-030` 조회/읽음 전이와 `UC-032` 생성 규칙 자체를 재설계하지 않는다.
- canonical 설계 문서, completed plan 경로, 비관련 dirty worktree 파일을 수정하지 않는다.

## 실행 경계
- 대상 bounded context/module: `Notification Management Context` / `notification`
- 대상 aggregate root: `org.codenbug.notification.domain.entity.Notification`
### 수정 허용 경로
- `notification/src/main/java/org/codenbug/notification/ui/**`
- `notification/src/main/java/org/codenbug/notification/application/**`
- `notification/src/main/java/org/codenbug/notification/domain/**`
- `notification/src/main/java/org/codenbug/notification/infra/**`
- `notification/src/test/java/org/codenbug/notification/**`
- `scripts/run-app-infra.sh`
- `scripts/check-app-infra.sh`
- `scripts/run-app-server.sh`
- 현재 `docs/use-cases/UC-031/affected-files.md`는 legacy taxonomy(`controller`, `application/service`, `infrastructure`)를 가리켜 이 계획의 실제 `ui`, `application`, `domain`, `infra` 경계와 충돌한다. 다음 executor rerun 전 control-plane scope artifact는 이 계획 경계를 기준으로 다시 materialize되어야 한다.
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
- `notification/src/main/java/org/codenbug/notification/ui/NotificationCommandController.java`
- `notification/src/main/java/org/codenbug/notification/ui/dto/NotificationDeleteRequestDto.java`
- `notification/src/main/java/org/codenbug/notification/application/NotificationCommandService.java`
- `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`
- `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`
- `notification/src/test/java/org/codenbug/notification/application/NotificationCommandServiceIdempotencyTest.java`
- `notification/src/test/java/org/codenbug/notification/domain/NotificationDomainServiceTest.java`

## 패키지 및 의존성 계약
### 생성/수정 클래스와 정확한 package
- 수정 `org.codenbug.notification.ui.NotificationCommandController`: authenticated delete endpoint는 유지하고 삭제 scope별 request mapping만 adapter로 조정한다.
- 수정 `org.codenbug.notification.ui.dto.NotificationDeleteRequestDto`: `selected-set` request shape validation만 담당한다.
- 수정 `org.codenbug.notification.application.NotificationCommandService`: single, `selected-set`, `all-owned` 삭제 orchestration과 transaction 경계를 소유한다.
- 수정 `org.codenbug.notification.application.port.NotificationStore`: selected ownership 검증용 existing requested-ID read 계약을 추가한다.
- 생성 `org.codenbug.notification.domain.entity.NotificationSelection`: 중복 제거된 requested notification ID 집합을 표현한다.
- 생성 `org.codenbug.notification.domain.NotificationDeletionPolicy`: foreign-owned 포함 거절, missing owned 제외, remaining existing owned 추리기 정책을 소유한다.
- 수정 `org.codenbug.notification.domain.NotificationDomainService`: 단건 ownership 검증은 유지하되 selected 집합 정책은 새 domain policy로 분리한다.
- 수정 `org.codenbug.notification.domain.entity.Notification`: `UserId` ownership 비교용 메서드를 추가하거나 동등 책임을 부여한다.
- 수정 `org.codenbug.notification.infra.NotificationStoreAdapter`
- 수정 `org.codenbug.notification.infra.NotificationRepository`
- 생성 또는 수정 테스트
- `org.codenbug.notification.domain.NotificationDeletionPolicyTest`
- `org.codenbug.notification.application.NotificationCommandServiceDeleteTest`
- `org.codenbug.notification.ui.NotificationCommandControllerDeleteTest`
- `org.codenbug.notification.infra.NotificationStoreAdapterDeleteTest`
### 각 클래스의 layer와 책임
- `ui`: `LoggedInUserContext`, validation annotation, HTTP status, `RsData` 생성만 담당한다.
- `application`: `NotificationCommandService`가 `find -> ownership/policy evaluation -> delete` 순서로 use case를 조합한다.
- `application`: `NotificationCommandService`가 `requesterId`, `deletionScope`, `requestedCount`, `deletedCount`, `rejectionReasonCategory`만 담는 structured application log를 남긴다. notification content, title, targetUrl, secret 값은 log에 넣지 않는다.
- `domain.entity`: `Notification`, `UserId`, `NotificationSelection`이 invariant-friendly 값/상태를 보장한다.
- `domain`: `NotificationDeletionPolicy`, `NotificationDomainService`가 entity 하나를 넘는 정책과 생성/소유권 규칙을 제공한다.
- `infra`: JPA repository/query/delete 구현만 담당하고 도메인 거절 판단을 하지 않는다.
### 허용 의존성 방향
- `ui` -> `application`
- `application` -> `application.port`, `domain`
- `infra` -> `application.port`, `domain`
- 테스트는 대상 layer와 test fixture에만 의존한다.
### 금지 import/framework dependency
- `application`/`domain`에서 `jakarta.servlet`, `org.springframework.web.*`, controller DTO 직접 의존 금지
- `domain`에서 `JpaRepository`, `ResponseEntity`, `LoggedInUserContext`, `RsData` 의존 금지
- `ui`에서 삭제 정책, ownership 판정, missing-owned 허용 규칙을 직접 구현 금지
- `app/`에 controller, repository, 비즈니스 삭제 규칙 추가 금지
### bootstrap/configuration wiring
- 새 wiring은 `notification` 모듈 내부 Spring component scan 범위에서 해결한다.
- `NotificationDeletionPolicy`가 Spring stereotype 없이 순수 도메인 객체로 남는다면 `NotificationCommandService`에서 직접 생성하지 말고 기존 `NotificationDomainService` 패턴과 일관되게 bean 등록 또는 생성 방식을 정한다.
- launcher script 변경은 gateway `8080` 경유 수동 검증에 꼭 필요할 때만 수행한다.

## 도메인 구현 계약
### Aggregate invariant
- 모든 삭제 결과는 인증된 요청자의 `UserId` ownership 범위 안의 `Notification`에만 적용된다.
- 단건 삭제와 `selected-set` 삭제는 existing foreign-owned `Notification`이 하나라도 포함되면 aggregate 변경 없이 거절된다.
- `selected-set` 삭제는 이미 없는 owned `Notification`을 실패 사유로 만들지 않고 remaining existing owned만 삭제한다.
- `all-owned` 삭제는 요청 시점의 requester-owned 전체 범위에만 적용된다.
### 상태 전이
- 삭제는 `NotificationStatus` 변경이 아니라 row hard delete다.
- `Deleted` 상태를 새로 만들지 않는다.
- `UC-030`의 `UNREAD -> READ` 전이 규칙은 유지되어야 하며 삭제 구현이 read-state semantics를 바꾸면 안 된다.
### Entity/Value Object 생성 및 검증 규칙
- `UserId`는 기존 trim/non-blank 규칙을 그대로 사용한다.
- `NotificationSelection`은 `notificationIds`를 비어 있지 않은 집합으로 정규화하고 duplicate ID는 제거한다.
- `NotificationDeleteRequestDto`는 boundary에서 non-empty를 검증하되, domain 쪽은 normalized `NotificationSelection`을 기준으로 판단한다.
- 단건 삭제는 `Notification` entity ownership 비교 후 거절 또는 삭제만 결정한다.
### Domain Service 여부와 책임
- `NotificationDomainService`는 생성/단건 ownership 검증 기존 책임 유지.
- `NotificationDeletionPolicy`는 `selected-set` 집합 정책 전담.
- `selected-set` 거절 규칙을 `NotificationStoreAdapter`나 controller에 두지 않는다.
### Domain Event 및 persistence compatibility
- 새 외부 발행 이벤트 추가 없음.
- 의미상 `NotificationDeleted`는 domain/e2e 증거 용어로만 유지한다.
- persistence는 기존 JPA `Notification` shape와 호환되어야 하고, `UC-030`/`UC-032` 공유 aggregate 속성을 깨면 안 된다.
- 삭제 observability는 새 tracing/metrics stack 없이 기존 SLF4J 패턴을 사용한다. 성공 path는 `scope`와 count를 남기고, 거절 path는 `rejectionReasonCategory`를 남기되 notification content와 secret은 남기지 않는다.
### 다른 Aggregate/Bounded Context 협력 방식
- 외부 시스템 협력 없음.
- 인증은 기존 `security-aop`/gateway 경계가 선행 책임을 가진다.
- 삭제 로직은 `Notification Management Context` 내부에서만 완료한다.
### Transaction, idempotency, concurrency
- 각 삭제 command 메서드는 Spring `@Transactional` 경계 하나 안에서 끝난다.
- `selected-set`은 requested existing rows fetch, foreign-owned 검증, remaining owned delete를 같은 transaction 안에서 처리한다.
- 이미 없는 owned ID는 idempotent success로 취급한다.
- pessimistic lock, distributed lock, retry loop 추가 금지.

## 외부 계약 읽기 허용 목록
- 모듈 로컬 규칙 및 focused test command contract -> `notification/AGENTS.md`
- 인증/요청자 식별 contract -> `security-aop/src/main/java/org/codenbug/securityaop/aop/**`
- 공통 HTTP 응답 wrapper/role annotation contract -> `platform/common/src/main/java/org/codenbug/common/**`
- gateway 기동 및 앱 런처 contract -> `scripts/run-app-infra.sh`
- gateway 기동 및 인프라 readiness contract -> `scripts/check-app-infra.sh`
- gateway 기동 및 서버 실행 contract -> `scripts/run-app-server.sh`
- test gate contract -> `.codex/test-gate.yaml`
- 아키텍처 정적 규칙 contract -> `.semgrep/ddd-architecture.yml`

## 작업 체크리스트
- [ ] TASK-001 `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationSelection.java`: non-empty selected ID 집합 정규화와 duplicate 제거 규칙을 구현한다.
- [ ] TASK-002 `notification/src/main/java/org/codenbug/notification/domain/NotificationDeletionPolicy.java`: foreign-owned 포함 시 전체 거절, missing owned 제외, zero remaining existing owned 정상 종료 규칙을 구현한다.
- [ ] TASK-003 `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`, `notification/src/main/java/org/codenbug/notification/domain/NotificationDomainService.java`: 단건 ownership 판정용 메서드/검증 책임을 aggregate 규칙과 맞춘다.
- [ ] TASK-004 `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationRepository.java`, `notification/src/main/java/org/codenbug/notification/infra/NotificationStoreAdapter.java`: requester scope 없이 requested IDs existing row를 읽는 계약과 hard delete 호출 경로를 추가한다.
- [ ] TASK-005 `notification/src/main/java/org/codenbug/notification/application/NotificationCommandService.java`: single, `selected-set`, `all-owned` 삭제 흐름을 `find -> validate -> delete` 순서로 재구성하고 transaction 안에서 마무리한다.
- [ ] TASK-005A `notification/src/main/java/org/codenbug/notification/application/NotificationCommandService.java`: `single`, `selected-set`, `all-owned` 각 경로에서 `requesterId`, `deletionScope`, `requestedCount`, `deletedCount`, `rejectionReasonCategory` 필드를 가진 structured application log를 추가하고 content/title/secret 미노출 규칙을 지킨다.
- [ ] TASK-006 `notification/src/main/java/org/codenbug/notification/ui/dto/NotificationDeleteRequestDto.java`, `notification/src/main/java/org/codenbug/notification/ui/NotificationCommandController.java`: controller를 adapter-only로 유지하면서 normalized selected 삭제 요청을 application으로 전달한다.
- [ ] TEST-001 `notification/src/test/java/org/codenbug/notification/domain/NotificationDeletionPolicyTest.java`: duplicate normalization, foreign-owned rejection, missing owned ignore, zero-remaining success를 검증한다.
- [ ] TEST-002 `notification/src/test/java/org/codenbug/notification/application/NotificationCommandServiceDeleteTest.java`: single owned delete, single missing rejection, single foreign-owned rejection, selected partial delete, selected foreign-owned 전체 거절, all-owned delete를 검증한다.
- [ ] TEST-003 `notification/src/test/java/org/codenbug/notification/ui/NotificationCommandControllerDeleteTest.java`: unauthenticated rejection, authenticated delete endpoint mapping, DTO validation을 검증한다.
- [ ] TEST-004 `notification/src/test/java/org/codenbug/notification/infra/NotificationStoreAdapterDeleteTest.java`, `notification/src/test/java/org/codenbug/notification/application/NotificationApplicationServicePortTest.java`: port 기반 selected lookup 계약과 infra 의존성 방향을 검증한다.
- [ ] TASK-007 `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`: gateway `8080` 삭제 검증에 부족한 launcher contract가 있을 때만 최소 수정한다.
- [ ] TASK-008 `docs/plans/active/UC-031/plan.md`: 구현 후 실제 실행한 검증 결과와 범위 밖 기존 실패 여부를 `## 11. 검증 결과`에 반영한다.

## 집중 검증
- [ ] VERIFY-001 Build: `./gradlew :notification:build --no-daemon --console=plain` -> `notification` 모듈 compile/test/package 성공
- [ ] VERIFY-002 Focused tests: `./gradlew :notification:test --no-daemon --console=plain` -> `UC-031` 관련 domain/application/ui/infra 테스트 통과
- [ ] VERIFY-003 Architecture test: `./gradlew architectureRules --no-daemon --console=plain` -> `notification` 변경으로 인한 아키텍처 규칙 위반 0건
- [ ] VERIFY-004 E2E 또는 maintenance verification: `./gradlew :notification:test --no-daemon --console=plain` -> `NotificationCommandControllerDeleteTest`, `NotificationCommandServiceDeleteTest`가 포함된 focused suite에서 authenticated delete endpoint mapping, owned single/selected/all-owned delete, foreign-owned 전체 거절, missing-owned ignore가 통과
- [ ] VERIFY-005 Test gate: `.codex/test-gate.yaml` required stage PASS -> `required: []` 확인 및 추가 강제 gate 없음 기록
- [ ] VERIFY-006 Runtime server verification: `python3 -m harness_codex run app status`, `python3 -m harness_codex run app --foreground`, `curl -fsS http://127.0.0.1:9000/actuator/health`, `curl -fsS http://127.0.0.1:8080/actuator/health` -> launcher contract로 app `9000`, gateway `8080` health가 모두 `UP`
- [ ] VERIFY-007 Static analysis: `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java` -> blocking finding 0건
### 중단 조건
- `NotificationSelection`, `NotificationDeletionPolicy`, selected existing-ID fetch 계약 중 하나라도 정확한 package/책임 없이 구현되려 하면 중단한다.
- `application` 또는 `domain` 계층이 web type, controller DTO, security context에 직접 의존하려 하면 중단한다.
- foreign-owned 포함 시 전체 거절 규칙과 missing owned ignore 규칙을 동시에 만족시키지 못하면 중단한다.
- 삭제 outcome log가 요청자 식별/범위/건수/거절 분류 없이 임의 문장만 남기거나 notification content/secret을 노출하려 하면 중단한다.
- `UC-030` 조회/읽음 전이 또는 `UC-032` 생성 회귀가 발생하면 중단하고 원인 기록한다.
- runtime materialized scope가 `notification/.../ui`, `application`, `domain`, `infra` 실제 계획 경로를 막으면 executor 시작 전에 중단하고 scope artifact 재생성을 요청한다.

## 9. OWASP Security Review
- pending `security_plan_reviewer`; attack surface: authenticated delete endpoints, `notificationIds` request body, path `id`, ownership rejection path, requester/logged-in identity, delete result logging

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
- Notes: pending
