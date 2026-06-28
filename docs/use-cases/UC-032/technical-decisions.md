# UC-032. Technical Decisions

## 1. Metadata
|Item|Value|
|---|---|
|ChangeSet|CHG-20260625-001|
|Use Case|UC-032|
|Approval Status|approved|
|Approved By|runtime technical-decisions stage; no pending technical mechanism decisions|

## 2. Input Documents
|Document|Status|Use|
|---|---|---|
|docs/changes/active/CHG-20260625-001.md|available|ChangeSet scope and runtime stage state.|
|docs/use-cases/UC-032/use-case.md|available|Create-notification actor, success flow, failure flow, and required input.|
|docs/use-cases/UC-032/event-storming.md|available|Command/event flow, invariants, and lack of external-system collaboration.|
|docs/use-cases/UC-032/ddd-design.md|available|Candidate aggregate, domain service, application service, port, and BC communication model.|
|docs/use-cases/UC-032/e2e-goal.md|available|E2E success and failure verification expectations.|
|docs/changes/active/CHG-20260625-001.ddd-integration.md|available|Accepted canonical NotificationAggregate contract shared by UC-030, UC-031, and UC-032.|
|docs/changes/active/CHG-20260625-001.ddd-integration.json|available|Machine-readable accepted canonical model and invariants.|
|ARCHITECTURE.md|available|No detailed architecture beyond repository root title; implementation decisions follow existing notification module structure.|

## 3. Approved Decisions
|Decision Area|Decision|Rationale|Implementation Impact|Test/Verification Impact|
|---|---|---|---|---|
|API entrypoint and adapter|Use the existing Spring MVC `NotificationCommandController` under `/api/v1/notifications` as the create command adapter.|UC-032 requires authenticated API creation, and the module already exposes command endpoints through Spring MVC.|Keep web concerns in the controller. Controller maps `NotificationCreateRequestDto` to `NotificationCommandService.createNotification(...)` and returns the created DTO.|Controller tests must cover success, unauthenticated rejection, USER-role rejection, and validation failure paths where feasible with existing security test support.|
|Authentication and authorization mechanism|Use existing `@AuthNeeded` and `@RoleRequired({Role.ADMIN, Role.MANAGER})` AOP/security annotations on the create endpoint.|DDD integration assigns authentication and creation role checks to Access Control Context before Notification Management Context handles the command.|Do not duplicate role checks in domain or application services. Application service assumes caller already passed the access-control adapter.|Tests must verify ADMIN/MANAGER allowed and USER rejected at API/security boundary, not by domain logic.|
|Application transaction boundary|Use one synchronous `@Transactional` application-service method for validate, create, persist, event publication, and DTO mapping.|UC-032 requires exactly one unread notification persisted for a valid request and no persistence for validation failure. Current `NotificationCommandService` already has a transactional command boundary.|`NotificationCommandService.createNotification` remains the write transaction boundary. Domain validation exceptions roll back before save. No nested transaction or saga needed.|Application-service tests must assert one saved notification for valid input and no save on invalid input.|
|Persistence technology|Use Spring Data JPA with existing `NotificationRepository` and `NotificationStore` port adapter.|Notification is already a JPA aggregate root and DDD design names `NotificationStore.save` as the persistence port.|Persist through `NotificationStore.save(Notification)`. Do not bypass the port from application service. Keep JPA repository in infrastructure.|Repository/adapter tests should verify save and recipient-scoped query visibility using existing JPA setup.|
|Domain creation mechanism|Use `NotificationDomainService.createNotification(...)` to construct a `Notification` with `UserId`, `NotificationContent`, `NotificationType`, initial unread state, and optional `targetUrl`.|Canonical contract requires a valid create request to build exactly one unread Notification; existing domain service and VOs own validation and normalization.|Ensure content requiredness from UC-032 is enforced before persistence. `UserId` rejects blank IDs; `NotificationContent` must reject blank title and content for this slice.|Domain tests must cover blank recipient ID, missing/blank title, missing/blank content, optional target URL, and initial unread state.|
|Read-state representation|Use current persisted boolean `isRead=false` as the implementation representation for canonical `NotificationReadState.UNREAD` in this slice.|DDD integration normalizes read state conceptually, while current schema uses `isRead`; UC-032 only needs initial unread creation.|Creation must set `isRead` to false. Planner may introduce a VO wrapper only if needed across the accepted ChangeSet, but UC-032 does not require a schema change by itself.|Tests must assert created notification is unread through DTO/query-visible state.|
|Inbox visibility|Treat Notification Inbox as the existing recipient-scoped query view over persisted notifications, not a separate table or entity.|DDD integration explicitly accepts Inbox as a derived recipient-scoped query view.|After save, no additional inbox row is written. Visibility comes from existing recipient and unread/latest query methods.|E2E/integration verification must create a notification, then query the recipient inbox or unread inbox and find the created notification.|
|Messaging/outbox/idempotency|Do not add outbox, inbox, message broker, scheduler, idempotency key, or retry for the direct UC-032 authenticated create endpoint.|Event storming says this use case has no external-system collaboration. Direct API creation must persist one notification synchronously.|Existing local Spring event publication may remain for in-process notification emission, but persistence success is not made dependent on broker delivery. `sourceKey` idempotency remains for event-listener flows outside this slice.|Tests for UC-032 should not require RabbitMQ or asynchronous delivery. Verify database state and API/query observability.|
|Cache policy|Do not introduce cache for creation or inbox visibility.|UC-032 requires immediate persisted visibility; cache would add invalidation work without slice evidence.|Reads after write must hit repository-backed query behavior or existing uncached projection path.|Verification should avoid cache assumptions and assert state through repository/API.|
|Failure handling|Use fail-fast validation and transaction rollback for invalid input; use security-layer rejection for unauthenticated or unauthorized calls.|Failure flows require rejection without storing a Notification.|Controller validation handles missing DTO fields; domain VOs/services handle blank or normalized invalid values. No compensation flow is needed because no external side effect is required for success.|Tests must assert invalid requests do not increase notification count.|
|Observability|Use existing service logging around create start and event publication; do not add new metrics/tracing dependency for this slice.|No required observability tooling is specified beyond API/runtime observable behavior in the E2E goal.|Keep logs free of secrets. User ID, notification type, title, and generated notification ID are acceptable at debug level if current module already logs them.|Verification relies on API response and query-visible persisted data, not log inspection.|
|Database migration strategy|No new table is required for UC-032. Add a migration only if implementation changes column nullability or constraints to enforce required `content` consistently.|Current `notification` table model already stores Notification root fields needed by UC-032.|Planner must inspect current schema/migrations before deciding whether to tighten content nullability or validation only in application/domain code.|If a migration is added, include persistence tests proving valid create succeeds and invalid content is rejected before save.|

## 4. Failure, Recovery, and Consistency Policy
|Situation|Policy|Retry/Compensation|Observability|Required Tests|
|---|---|---|---|---|
|Unauthenticated create request|Reject at Access Control Context before application service.|No retry or compensation.|Security response is API-visible.|Controller/security test for unauthenticated rejection and unchanged notification count.|
|USER role create request|Reject at Access Control Context because only ADMIN or MANAGER may create.|No retry or compensation.|Security response is API-visible.|Controller/security test for USER-role rejection and unchanged notification count.|
|Blank Recipient User ID|Reject before persistence through request/domain validation.|No retry or compensation.|Validation failure is API/runtime visible.|DTO/domain/application test showing no notification saved.|
|Missing or blank required title/content/type|Reject before persistence through DTO and domain validation.|No retry or compensation.|Validation failure is API/runtime visible.|DTO/domain/application test showing no notification saved.|
|Valid create request|Persist exactly one Notification with unread state in one transaction.|No retry or compensation; caller may submit a new request if transport fails before response.|201 response with created DTO; recipient inbox query later exposes notification.|Application or E2E test proving one row is created, unread, and recipient-scoped inbox can retrieve it.|
|Local event publication after save|Treat as in-process notification emission support, not the source of truth for creation success.|Do not add outbox/retry for UC-032 direct API create.|Debug log may include generated notification ID.|Tests should not require asynchronous event delivery to satisfy UC-032.|

## 5. Planner Requirements
- Decisions planner must include:
  - Keep create command flow inside `notification` module using controller -> application service -> domain service -> store adapter.
  - Verify `@AuthNeeded` and `@RoleRequired({Role.ADMIN, Role.MANAGER})` protect the create endpoint.
  - Ensure required `content` is enforced for UC-032 before persistence, aligning DTO/domain/schema behavior if current code allows null content.
  - Verify created notification is unread and visible through recipient-scoped inbox or unread-inbox query.
  - Add focused tests for ADMIN/MANAGER success, USER rejection, unauthenticated rejection, blank recipient ID, missing required fields, optional target URL, and no-save failure behavior.
- Decisions executor must not change:
  - Do not add a persisted Inbox entity for UC-032.
  - Do not add RabbitMQ, scheduler, outbox, retry, idempotency, or cache mechanics to direct authenticated create.
  - Do not move web/security types into domain or application-layer method contracts.
  - Do not reinterpret `NotificationStatus` as canonical read state for this ChangeSet.
- Tests/verification planner must include:
  - `./gradlew :notification:test`
  - Targeted controller/application/domain tests if the full module test suite is too broad during iteration.
  - A verification note explaining how successful create and subsequent inbox visibility were observed.

## 6. Slice-First External Lookup Record
|Outside Document|Why Read|Missing Slice Information|Conflict|Handling|
|---|---|---|---|---|
|notification/AGENTS.md|Needed module-local rules and verification command.|None.|None.|Followed notification module ownership and `./gradlew :notification:test` guidance.|
|notification source files under `src/main`|Needed current framework, persistence, validation, and controller patterns because `ARCHITECTURE.md` has no detailed guidance.|None.|Current `NotificationContent` permits null content while canonical UC-032 requires content.|Planner must enforce required content before persistence; this is an implementation task, not a technical-decision blocker.|

## 7. Pending Decisions
- None
