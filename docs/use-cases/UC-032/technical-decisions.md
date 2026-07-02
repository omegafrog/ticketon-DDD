# UC-032. 기술 결정

## 1. 메타데이터
|항목|값|
|---|---|
|ChangeSet|CHG-20260625-001|
|Use Case|UC-032|
|Approval Status|approved|
|승인 근거|runtime technical-decisions stage에서 확정했고, 보류 중인 기술 mechanism 결정이 없다.|

## 2. 입력 문서
|문서|상태|사용 목적|
|---|---|---|
|docs/changes/active/CHG-20260625-001.md|확인됨|ChangeSet 범위와 runtime stage state 확인.|
|docs/use-cases/UC-032/use-case.md|확인됨|Create-notification actor, success flow, failure flow, required input 확인.|
|docs/use-cases/UC-032/event-storming.md|확인됨|Command/event flow, invariant, external-system collaboration 부재 확인.|
|docs/use-cases/UC-032/ddd-design.md|확인됨|Candidate aggregate, domain service, application service, port, BC communication model 확인.|
|docs/use-cases/UC-032/e2e-goal.md|확인됨|E2E success와 failure verification expectation 확인.|
|docs/changes/active/CHG-20260625-001.ddd-integration.md|확인됨|UC-030, UC-031, UC-032가 공유하는 accepted canonical NotificationAggregate contract 확인.|
|docs/changes/active/CHG-20260625-001.ddd-integration.json|확인됨|기계 판독 가능한 accepted canonical model과 invariant 확인.|
|ARCHITECTURE.md|확인됨|Repository root title 외 상세 architecture가 없어 기존 notification module structure를 따른다.|

## 3. 승인된 결정
|결정 영역|결정|근거|구현 영향|테스트/검증 영향|
|---|---|---|---|---|
|API entrypoint and adapter|Create command adapter는 `/api/v1/notifications` 아래의 기존 Spring MVC `NotificationCommandController`를 사용한다.|UC-032는 authenticated API creation을 요구하고, 모듈은 이미 Spring MVC를 통해 command endpoint를 노출한다.|Web concern은 controller에 둔다. Controller는 `NotificationCreateRequestDto`를 `NotificationCommandService.createNotification(...)` 호출로 mapping하고 created DTO를 반환한다.|Controller test는 success, unauthenticated rejection, USER-role rejection, 기존 security test support가 가능한 validation failure path를 다뤄야 한다.|
|Authentication and authorization mechanism|Create endpoint에는 기존 `@AuthNeeded`와 `@RoleRequired({Role.ADMIN, Role.MANAGER})` AOP/security annotation을 사용한다.|DDD integration은 authentication과 creation role check를 Notification Management Context가 command를 처리하기 전 Access Control Context에 둔다.|Domain 또는 application service에서 role check를 중복하지 않는다. Application service는 caller가 access-control adapter를 통과했다고 가정한다.|Test는 ADMIN/MANAGER가 허용되고 USER가 domain logic이 아니라 API/security boundary에서 거절되는지 검증해야 한다.|
|Application transaction boundary|Validate, create, persist, event publication, DTO mapping은 하나의 synchronous `@Transactional` application-service method로 처리한다.|UC-032는 valid request에 대해 정확히 하나의 unread notification persist를 요구하고, validation failure에는 persistence가 없어야 한다. 현재 `NotificationCommandService`도 transactional command boundary를 가진다.|`NotificationCommandService.createNotification`은 write transaction boundary로 유지한다. Domain validation exception은 save 전에 rollback된다. Nested transaction 또는 saga는 필요 없다.|Application-service test는 valid input에서 notification 하나가 저장되고 invalid input에서는 save가 없음을 검증해야 한다.|
|Persistence technology|기존 `NotificationRepository`와 `NotificationStore` port adapter 기반 Spring Data JPA를 사용한다.|Notification은 이미 JPA aggregate root이고 DDD design은 persistence port로 `NotificationStore.save`를 명명한다.|Application service는 `NotificationStore.save(Notification)`를 통해 persist한다. Port를 우회하지 않는다. JPA repository는 infra layer에 둔다.|Repository/adapter test는 기존 JPA setup을 사용해 save와 recipient-scoped query visibility를 검증해야 한다.|
|Domain creation mechanism|`NotificationDomainService.createNotification(...)`으로 `UserId`, `NotificationContent`, `NotificationType`, initial unread state, optional `targetUrl`을 가진 `Notification`을 생성한다.|Canonical contract는 valid create request가 정확히 하나의 unread Notification을 만들어야 한다고 요구한다. 기존 domain service와 VO가 validation과 normalization을 소유한다.|UC-032의 content requiredness는 persistence 전에 강제해야 한다. `UserId`는 blank ID를 거부하고, `NotificationContent`는 이 slice에서 blank title/content를 거부해야 한다.|Domain test는 blank recipient ID, missing/blank title, missing/blank content, optional target URL, initial unread state를 다뤄야 한다.|
|Read-state representation|Canonical `NotificationReadState.UNREAD`의 구현 표현은 현재 persisted boolean `isRead=false`를 사용한다.|DDD integration은 read state를 개념적으로 normalize했고 현재 schema는 `isRead`를 쓴다. UC-032는 initial unread creation만 필요하다.|Creation은 `isRead`를 false로 설정해야 한다. Planner는 accepted ChangeSet 전체에 필요할 때만 VO wrapper를 도입할 수 있으며, UC-032 단독으로 schema change를 요구하지 않는다.|Test는 생성된 notification이 DTO/query-visible state에서 unread임을 확인해야 한다.|
|Inbox visibility|Notification Inbox는 별도 table 또는 entity가 아니라 persisted notification 위의 기존 recipient-scoped query view로 취급한다.|DDD integration은 Inbox를 derived recipient-scoped query view로 승인했다.|Save 이후 별도 inbox row를 쓰지 않는다. Visibility는 기존 recipient/unread/latest query method에서 나온다.|E2E/integration verification은 notification을 생성한 뒤 recipient inbox 또는 unread inbox를 query해서 created notification을 찾아야 한다.|
|Messaging/outbox/idempotency|Direct UC-032 authenticated create endpoint에는 outbox, inbox, message broker, scheduler, idempotency key, retry를 추가하지 않는다.|Event storming은 이 use case에 external-system collaboration이 없다고 말한다. Direct API creation은 notification 하나를 synchronous하게 persist해야 한다.|기존 local Spring event publication은 in-process notification emission support로 남을 수 있지만, broker delivery가 persistence success의 조건이 되면 안 된다. `sourceKey` idempotency는 이 slice 밖 event-listener flow에 남긴다.|UC-032 test는 RabbitMQ 또는 asynchronous delivery를 요구하지 않아야 한다. Database state와 API/query observability를 검증한다.|
|Cache policy|Creation 또는 inbox visibility에 cache를 도입하지 않는다.|UC-032는 immediate persisted visibility를 요구한다. Cache는 slice evidence 없이 invalidation work를 늘린다.|Write 이후 read는 repository-backed query behavior 또는 기존 uncached projection path를 사용해야 한다.|Verification은 cache assumption 없이 repository/API를 통해 state를 확인해야 한다.|
|Failure handling|Invalid input은 fail-fast validation과 transaction rollback으로 처리하고, unauthenticated/unauthorized call은 security-layer rejection으로 처리한다.|Failure flow는 Notification을 저장하지 않는 rejection을 요구한다.|Controller validation은 missing DTO field를 처리하고, domain VO/service는 blank 또는 normalized invalid value를 처리한다. Success에 필요한 external side effect가 없으므로 compensation flow도 필요 없다.|Test는 invalid request가 notification count를 늘리지 않는지 확인해야 한다.|
|Observability|Create start와 event publication 주변에는 기존 service logging을 사용한다. 이 slice에는 새 metrics/tracing dependency를 추가하지 않는다.|E2E goal의 API/runtime observable behavior 외 필수 observability tooling은 없다.|Log에는 secret을 남기지 않는다. 현재 module이 이미 logging한다면 user ID, notification type, title, generated notification ID는 debug level에서 허용할 수 있다.|Verification은 log inspection이 아니라 API response와 query-visible persisted data에 의존한다.|
|Database migration strategy|UC-032에는 새 table이 필요 없다. Current code가 `content` requiredness를 일관되게 강제하려고 column nullability 또는 constraint를 바꾸는 경우에만 migration을 추가한다.|현재 `notification` table model은 UC-032에 필요한 Notification root field를 이미 저장한다.|Planner는 content nullability를 강화할지, application/domain validation만으로 충분한지 결정하기 전에 현재 schema/migration을 확인해야 한다.|Migration이 추가되면 valid create가 성공하고 invalid content가 save 전에 거절됨을 증명하는 persistence test를 포함해야 한다.|

## 4. 실패, 복구, 일관성 정책
|상황|정책|재시도/보상|관측성|필수 테스트|
|---|---|---|---|---|
|Unauthenticated create request|Application service 전에 Access Control Context에서 reject한다.|Retry 또는 compensation 없음.|Security response가 API-visible이다.|Unauthenticated rejection과 unchanged notification count에 대한 controller/security test.|
|USER role create request|ADMIN 또는 MANAGER만 생성할 수 있으므로 Access Control Context에서 reject한다.|Retry 또는 compensation 없음.|Security response가 API-visible이다.|USER-role rejection과 unchanged notification count에 대한 controller/security test.|
|Blank Recipient User ID|Request/domain validation으로 persistence 전에 reject한다.|Retry 또는 compensation 없음.|Validation failure가 API/runtime visible이다.|Notification이 저장되지 않음을 보여주는 DTO/domain/application test.|
|Missing or blank required title/content/type|DTO와 domain validation으로 persistence 전에 reject한다.|Retry 또는 compensation 없음.|Validation failure가 API/runtime visible이다.|Notification이 저장되지 않음을 보여주는 DTO/domain/application test.|
|Valid create request|하나의 transaction에서 unread state의 Notification을 정확히 하나 persist한다.|Retry 또는 compensation 없음. Transport failure가 response 전에 발생하면 caller가 새 request를 보낼 수 있다.|Created DTO를 담은 201 response. 이후 recipient inbox query가 notification을 노출한다.|한 row가 생성되고 unread이며 recipient-scoped inbox에서 조회됨을 증명하는 application 또는 E2E test.|
|Local event publication after save|In-process notification emission support로 취급하고 creation success의 source of truth로 삼지 않는다.|UC-032 direct API create에는 outbox/retry를 추가하지 않는다.|Debug log는 generated notification ID를 포함할 수 있다.|UC-032 만족을 위해 asynchronous event delivery를 요구하지 않아야 한다.|

## 5. 계획 작성 요구사항
- 계획 작성자가 포함해야 할 결정:
  - Controller -> application service -> domain service -> store adapter 흐름으로 `notification` module 안에 create command flow를 유지한다.
  - Create endpoint가 `@AuthNeeded`와 `@RoleRequired({Role.ADMIN, Role.MANAGER})`로 보호되는지 검증한다.
  - 현재 code가 null content를 허용한다면 UC-032에 맞춰 required `content`가 persistence 전에 강제되도록 DTO/domain/schema behavior를 맞춘다.
  - Created notification이 unread이고 recipient-scoped inbox 또는 unread-inbox query에서 보이는지 검증한다.
  - ADMIN/MANAGER success, USER rejection, unauthenticated rejection, blank recipient ID, missing required fields, optional target URL, no-save failure behavior에 대한 focused test를 추가한다.
- 구현 실행자가 변경하면 안 되는 결정:
  - UC-032를 위해 persisted Inbox entity를 추가하지 않는다.
  - Direct authenticated create에 RabbitMQ, scheduler, outbox, retry, idempotency, cache mechanics를 추가하지 않는다.
  - Web/security type을 domain 또는 application-layer method contract로 옮기지 않는다.
  - 이 ChangeSet에서 `NotificationStatus`를 canonical read state로 재해석하지 않는다.
- 테스트/검증 계획에 포함해야 할 항목:
  - `./gradlew :notification:test`
  - Iteration 중 full module test suite가 너무 넓으면 targeted controller/application/domain test를 우선 실행한다.
  - Successful create와 subsequent inbox visibility를 어떻게 관찰했는지 verification note에 기록한다.

## 6. Slice 우선 외부 조회 기록
|외부 문서|조회 이유|Slice에 없던 정보|충돌|처리|
|---|---|---|---|---|
|notification/AGENTS.md|Module-local rule과 verification command가 필요했다.|없음.|없음.|Notification module ownership과 `./gradlew :notification:test` guidance를 따랐다.|
|notification source files under `src/main`|`ARCHITECTURE.md`에 상세 guidance가 없어 현재 framework, persistence, validation, controller pattern이 필요했다.|없음.|현재 `NotificationContent`는 null content를 허용하지만 canonical UC-032는 content를 요구한다.|Planner는 persistence 전에 required content를 강제해야 한다. 이는 implementation task이며 technical-decision blocker는 아니다.|

## 7. 보류 중인 결정
- 없음
