# UC-031. 기술 결정

## 1. 메타데이터
|항목|값|
|---|---|
|ChangeSet|CHG-20260625-001|
|Use Case|UC-031|
|Approval Status|approved|
|승인 근거|runtime technical-decisions stage에서 확정했고, 구현을 막는 기술적 모호성이 없다.|

## 2. 입력 문서
|문서|상태|사용 목적|
|---|---|---|
|docs/changes/active/CHG-20260625-001.md|확인됨|ChangeSet 범위와 stage state 확인.|
|docs/use-cases/UC-031/use-case.md|확인됨|Deletion scope, ownership isolation, missing selected owned handling 확인.|
|docs/use-cases/UC-031/event-storming.md|확인됨|Command, event, invariant, failure-flow evidence 확인.|
|docs/use-cases/UC-031/ddd-design.md|확인됨|Candidate application service, store port, VO, policy shape 확인.|
|docs/use-cases/UC-031/e2e-goal.md|확인됨|Gateway API verification target과 observable result 확인.|
|docs/changes/active/CHG-20260625-001.ddd-integration.md|확인됨|Canonical aggregate, hard-delete decision, shared invariant 확인.|
|docs/changes/active/CHG-20260625-001.ddd-integration.json|확인됨|기계 판독 가능한 canonical model과 invariant 확인.|
|ARCHITECTURE.md|확인됨|Repo rule 외 추가 project architecture constraint가 없음을 확인.|

## 3. 승인된 결정
|결정 영역|결정|근거|구현 영향|테스트/검증 영향|
|---|---|---|---|---|
|Application boundary|삭제는 기존 REST controller entrypoint 뒤의 `notification` application/domain/infra layer에서 구현한다.|Slice는 Notification Management Context 내부 작업이고, 인증된 traffic은 gateway와 기존 notification controller를 통해 들어온다.|Web DTO와 `LoggedInUserContext`는 controller code에만 둔다. Application service는 primitive ID, `UserId`, domain VO value를 받고 servlet/web type을 반환하지 않는다.|Controller test는 authenticated gateway-facing deletion call을 다루고, application test는 web dependency 없이 service behavior를 다뤄야 한다.|
|Persistence technology|기존 Spring Data JPA repository와 `NotificationStore` port를 사용한다. 새 persistence technology는 추가하지 않는다.|기존 aggregate는 JPA entity이고 integration contract는 soft state가 아니라 aggregate removal을 말한다.|Ownership-safe selected deletion에 필요한 경우에만 `NotificationStore`를 확장한다. 예: recipient scoping 없이 existing requested ID를 fetch하고 선택된 aggregate root를 한 transaction에서 delete한다.|Repository 또는 adapter test는 selected fetch, ownership filtering, hard deletion, subsequent inbox absence를 검증해야 한다.|
|Deletion model|JPA `delete`/`deleteAll` 기반 hard delete를 사용한다. `Deleted` state, soft-delete column, scheduler, retention, cleanup mechanism을 추가하지 않는다.|DDD integration은 aggregate removal을 명시적으로 승인했고, 이 ChangeSet에서 persistent deleted state를 거부한다.|승인된 scope의 persisted `Notification` row를 제거한다. 기존 `NotificationStatus`는 delivery status로 유지하고 deletion lifecycle에 사용하지 않는다.|Test는 삭제된 notification이 inbox/unread/detail query에 반환되지 않음을 확인한다. Cleanup 또는 expiry test는 필요 없다.|
|Selected-set ownership check|Requested ID 전체에 대해 existing notification을 fetch하고, existing notification 중 requester 소유가 아닌 것이 있으면 reject한다. 그 뒤 existing owned notification만 delete한다.|현재 recipient-scoped fetch는 foreign-owned와 missing selected ID를 구별할 수 없다. Slice는 foreign-owned 포함 시 reject하고 이미 missing인 owned ID는 ignore해야 한다.|`findAllByIdIn(List<Long>)`에 해당하는 port method를 추가하거나 사용한다. `NotificationSelection`에서 requested ID를 normalize하고, existing root와 requester `UserId`를 비교한 뒤 rejection check 통과 시 owned existing root만 delete한다.|Application test는 all-owned selected, existing foreign-owned 포함 rejection, already missing ID ignore, duplicate ID normalize, zero remaining existing owned success를 다뤄야 한다.|
|Single delete lookup|요청 notification을 ID로 fetch하고, missing 또는 non-owned를 delete 전에 reject한다.|Single delete는 selected-set의 missing-owned tolerance를 갖지 않으며, event storming은 deletion 전 ownership confirmation을 요구한다.|`findById`, `Notification.isOwnedBy(UserId)` 또는 같은 domain policy를 사용한 뒤 `delete`한다. Missing과 non-owned는 deletion 없이 rejection을 만든다.|Test는 owned single deletion, foreign-owned single rejection, missing single rejection, unrelated deletion 없음 여부를 다뤄야 한다.|
|All-owned deletion scope|Request 시점의 requester-owned notification을 fetch하고 같은 transaction 안에서 그 list를 delete한다.|Use case는 all-owned가 request 시점의 requester-owned notification에 적용된다고 말한다.|기존 `findByUserIdOrderBySentAtDesc(UserId)` 또는 recipient-scoped bulk-delete adapter를 도입한다면 그것을 사용한다. Deletion scope는 requester `UserId`로 제한한다.|Test는 여러 recipient를 seed하고 requester-owned row만 제거되는지 검증해야 한다.|
|Transaction mechanics|각 delete command는 하나의 Spring `@Transactional` application-service method 안에서 끝낸다.|Ownership validation과 delete는 local JPA use case 안에서 atomic해야 한다. Cross-service side effect는 없다.|Selected validation과 deletion을 별도 transaction으로 쪼개지 않는다. Selected deletion은 fetch, domain policy evaluation, delete를 하나의 transaction에서 수행한다.|Integration test는 rejection 시 requested/existing notification 전체가 변경되지 않는지 확인해야 한다.|
|Concurrency|이미 missing인 selected ID에 대해서는 idempotent hard-delete outcome에 의존한다. Pessimistic lock 또는 distributed lock은 추가하지 않는다.|Approved slice는 missing selected owned notification을 허용하고, lock을 요구하는 concurrent update policy를 갖지 않는다.|이 slice에 Redis lock, row lock, retry loop, version field를 도입하지 않는다. Selected row가 fetch와 delete 사이에 사라지면 JPA delete가 사용자-visible failure를 만들지 않도록 remaining/zero deletion result로 처리한다.|Service test는 missing-at-fetch behavior를 다룬다. Lock-specific test는 필요 없다.|
|Retry and circuit breaker|Local database deletion에 retry, circuit breaker, bulkhead, fallback을 추가하지 않는다.|이 작업은 local transactional database write이며 external API dependency가 없다.|Transaction/database failure는 기존 exception handling으로 전파한다. Resilience library를 추가하지 않는다.|Test는 transient infrastructure retry가 아니라 domain outcome에 집중한다.|
|Messaging/outbox|UC-031에는 outbox/inbox, broker topic, external deletion event publisher를 추가하지 않는다.|E2E goal은 API result와 inbox absence를 관찰한다. Event storming에도 external system collaboration이 없다.|`NotificationDeleted`는 이후 승인된 slice가 integration publication을 요구하기 전까지 domain/event-storming semantic event로만 둔다.|Deletion에는 message contract test가 필요 없다.|
|Cache|Deletion에 Redis/application cache를 추가하지 않는다.|이 slice에는 승인된 cache read model이 없다.|Deletion은 database에 직접 write한다. 기존 query read는 database state를 관찰해야 한다.|Verification은 deletion 이후 API/query absence를 확인한다.|
|Observability|Requester ID, deletion scope, requested count, deleted count, rejection reason category를 담는 structured application log를 사용한다. 새 tracing/metrics stack은 추가하지 않는다.|Repo는 이미 logging을 사용하고, 이 slice에는 API deletion outcome 진단 가능성이면 충분하다.|Notification content 또는 secret은 logging하지 않는다. ID/count는 적절한 debug/info level에 기록하고 rejection category는 sensitive payload 없이 기록한다.|기존 project pattern이 없는 한 log line을 test assertion으로 삼지 않는다. Manual verification은 concise log를 확인할 수 있다.|
|Validation|DTO boundary에서는 Bean Validation으로 non-empty selected ID를 검증하고, domain VO에서는 `UserId`/`NotificationSelection` normalization을 검증한다.|Controller는 request shape를 소유하고 domain은 invariant-friendly value construction을 소유한다.|Duplicate removal과 positive/non-null ID check는 기존 style에 맞춰 `NotificationSelection` 또는 boundary validation에 둔다. Application service는 normalized selection을 받는다.|Controller test는 invalid selected request rejection을 다루고, domain test는 selection normalization을 다뤄야 한다.|

## 4. 실패, 복구, 일관성 정책
|상황|정책|재시도/보상|관측성|필수 테스트|
|---|---|---|---|---|
|Unauthenticated deletion request|Application service 전에 gateway/security layer에서 reject한다.|Retry 또는 compensation 없음.|기존 auth failure logging/response handling을 사용한다.|Unauthenticated rejection에 대한 controller/gateway-facing test.|
|Single delete target missing|다른 notification을 삭제하지 않고 reject한다.|Retry 또는 compensation 없음.|Rejection category와 notification ID를 log한다.|Missing single target application test.|
|Single delete target foreign-owned|Target을 삭제하지 않고 reject한다.|Retry 또는 compensation 없음.|Ownership rejection category, requester ID, notification ID를 log한다.|Foreign-owned single rejection application test.|
|Selected-set contains existing foreign-owned notification|Selected request 전체를 reject하고 아무것도 delete하지 않는다.|Retry 또는 compensation 없음.|Requested count와 requester ID를 포함해 selected ownership rejection을 log한다.|Application test는 rejection 이후 owned requested row가 유지되는지 확인한다.|
|Selected-set contains missing IDs|Existing fetched row에 대한 foreign-owned check 후 missing ID는 ignore하고 remaining existing owned notification만 delete한다.|Retry 또는 compensation 없음.|Requested count와 deleted count를 log한다.|Partial selected deletion과 zero remaining existing owned success application test.|
|All-owned deletion with no owned notifications|Deleted count 0으로 succeed한다.|Retry 또는 compensation 없음.|All-owned delete count 0을 log한다.|Empty all-owned scope application test.|
|Database transaction fails|Transaction을 rollback하고 기존 exception handling으로 failure를 노출한다.|이 slice에는 application retry가 없다.|기존 framework/service handling으로 error log를 남긴다.|가능하면 integration 또는 repository-backed test로 rollback을 검증한다.|
|Concurrent selected deletion removes a requested row before this transaction deletes it|Foreign-owned existing row가 없으면 selected deletion에서는 already missing처럼 처리한다.|Lock 또는 retry 없음.|Deleted count가 requested count보다 낮을 수 있다.|구현이 explicit concurrency handling을 추가하지 않는 한 concurrency-specific test는 필요 없다.|

## 5. 계획 작성 요구사항
- 계획 작성자가 포함해야 할 결정: `NotificationSelection` value object 또는 동등한 normalization, `NotificationDeletionPolicy`, selected ownership rejection을 위한 unscoped existing-ID fetch port, hard-delete implementation, transaction-scoped service method, single/selected-set/all-owned deletion에 대한 gateway-facing REST coverage.
- 구현 실행자가 변경하면 안 되는 결정: ChangeSet product behavior, DDD bounded context, gateway-first traffic rule, soft-delete/retention policy, external messaging/outbox behavior, application layer가 web type에 의존하지 않는 규칙.
- 테스트/검증 계획에 포함해야 할 항목: domain policy test, 모든 UC-031 success/failure flow에 대한 application service test, selected fetch/delete behavior에 대한 repository/adapter test, gateway-facing deletion에 대한 controller/auth test, deleted owned notification이 inbox에서 사라지고 다른 recipient notification은 남는다는 E2E/API verification.

## 6. Slice 우선 외부 조회 기록
|외부 문서|조회 이유|Slice에 없던 정보|충돌|처리|
|---|---|---|---|---|
|notification source files|기존 port, service, controller, JPA adapter 선택을 확인했다.|누락된 slice policy는 없고 implementation mechanism detail만 필요했다.|기존 selected deletion은 recipient-scoped fetch를 사용해 existing foreign-owned ID를 reject할 수 없다.|Planner는 store port와 selected policy implementation을 조정해야 한다.|
|notification/AGENTS.md|Module-local rule과 verification command를 확인했다.|누락된 slice policy 없음.|충돌 없음.|Focused verification은 `./gradlew :notification:test`를 사용한다.|
|ARCHITECTURE.md|Required input이다.|추가 constraint 없음.|충돌 없음.|Architecture constraint는 repo AGENTS와 DDD integration contract를 따른다.|

## 7. 보류 중인 결정
- 없음
