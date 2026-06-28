# UC-031. Technical Decisions

## 1. Metadata
|Item|Value|
|---|---|
|ChangeSet|CHG-20260625-001|
|Use Case|UC-031|
|Approval Status|approved|
|Approved By|runtime technical-decisions stage, no blocking technical ambiguity|

## 2. Input Documents
|Document|Status|Use|
|---|---|---|
|docs/changes/active/CHG-20260625-001.md|available|ChangeSet scope and stage state|
|docs/use-cases/UC-031/use-case.md|available|Deletion scope, ownership isolation, missing selected owned handling|
|docs/use-cases/UC-031/event-storming.md|available|Command, event, invariant, and failure-flow evidence|
|docs/use-cases/UC-031/ddd-design.md|available|Candidate application service, store port, VO, and policy shape|
|docs/use-cases/UC-031/e2e-goal.md|available|Gateway API verification target and observable result|
|docs/changes/active/CHG-20260625-001.ddd-integration.md|available|Canonical aggregate, hard-delete decision, shared invariants|
|docs/changes/active/CHG-20260625-001.ddd-integration.json|available|Machine-readable canonical model and invariants|
|ARCHITECTURE.md|available|No additional project architecture constraints beyond repo rules|

## 3. Approved Decisions
|Decision Area|Decision|Rationale|Implementation Impact|Test/Verification Impact|
|---|---|---|---|---|
|Application boundary|Implement deletion in `notification` application/domain/infrastructure layers behind existing REST controller entrypoints.|The slice is internal to Notification Management Context, while authenticated traffic enters through the gateway and existing notification controllers.|Keep web DTOs and `LoggedInUserContext` in controller code only. Application service accepts primitive IDs, `UserId`, or domain VO values and returns a deletion result DTO/value, not servlet/web types.|Controller tests cover authenticated gateway-facing deletion calls; application tests cover service behavior without web dependencies.|
|Persistence technology|Use existing Spring Data JPA repository and `NotificationStore` port; no new persistence technology.|The existing aggregate is a JPA entity and integration contract says deletion removes the aggregate, not a soft state.|Extend `NotificationStore` only where required for ownership-safe selected deletion, such as fetching existing requested IDs without recipient scoping and deleting selected aggregate roots in one transaction.|Repository or adapter tests verify selected fetch, ownership filtering, hard deletion, and subsequent inbox absence.|
|Deletion model|Use hard delete via JPA `delete`/`deleteAll`; do not add `Deleted` state, soft-delete column, scheduler, retention, or cleanup mechanism.|DDD integration explicitly accepts aggregate removal and rejects a persistent deleted state for this ChangeSet.|Remove persisted `Notification` rows for approved scope. Existing `NotificationStatus` remains delivery status only and is not used for deletion lifecycle.|Tests assert deleted notifications are not returned by inbox/unread/detail queries; no tests for cleanup or expiry.|
|Selected-set ownership check|Fetch all existing notifications for requested IDs, reject if any existing notification is not owned by requester, then delete only existing owned notifications.|Current recipient-scoped fetch cannot distinguish foreign-owned from missing selected IDs. The slice requires foreign-owned inclusion to reject and already missing owned IDs to be ignored.|Add or use a port method equivalent to `findAllByIdIn(List<Long>)`. Normalize requested IDs in `NotificationSelection`, compare existing roots with requester `UserId`, and delete only owned existing roots after rejection check passes.|Application tests must cover all-owned selected, selected with existing foreign-owned rejection, selected with already missing IDs ignored, duplicate IDs normalized, and selected with zero remaining existing owned succeeding.|
|Single delete lookup|Fetch the requested notification by ID, reject missing or non-owned before delete.|Single delete does not have the selected-set missing-owned tolerance, and event storming requires ownership confirmation before deletion.|Use `findById`, `Notification.isOwnedBy(UserId)` or equivalent domain policy, then `delete`. Missing and non-owned both produce a rejection without deletion.|Tests cover owned single deletion, foreign-owned single rejection, missing single rejection, and no unrelated deletion.|
|All-owned deletion scope|Fetch requester-owned notifications at request time and delete that list inside the same transaction.|Use case states all-owned applies to requester-owned notifications at request time.|Use existing `findByUserIdOrderBySentAtDesc(UserId)` or a recipient-scoped bulk-delete adapter if introduced. Keep deletion scoped by requester `UserId`.|Tests seed multiple recipients and verify only requester-owned rows are removed.|
|Transaction mechanics|Keep each delete command in one Spring `@Transactional` application-service method.|Ownership validation and delete must be atomic enough for this local JPA use case. No cross-service side effect participates.|Do not split selected validation and deletion across separate transactions. For selected deletion, perform fetch, domain policy evaluation, and delete in one transaction.|Integration tests verify rejection leaves all requested/existing notifications unchanged.|
|Concurrency|Rely on idempotent hard-delete outcome for already missing selected IDs; no pessimistic lock or distributed lock.|The approved slice tolerates missing selected owned notifications and has no concurrent update policy requiring locks.|Do not introduce Redis lock, row lock, retry loop, or version field for this slice. If a selected row disappears between fetch and delete, JPA delete should not create a user-visible failure beyond zero/remaining deletion result.|Service tests cover missing-at-fetch behavior; no lock-specific tests required.|
|Retry and circuit breaker|No retry, circuit breaker, bulkhead, or fallback for local database deletion.|The operation is a local transactional database write and has no external API dependency.|Let transaction/database failures propagate through existing exception handling. Do not add resilience libraries.|Tests focus on domain outcomes, not transient infrastructure retries.|
|Messaging/outbox|Do not add outbox/inbox, broker topic, or external deletion event publisher for UC-031.|The E2E goal observes API result and inbox absence; no external system collaboration exists in event storming.|`NotificationDeleted` remains a domain/event-storming semantic event unless a later approved slice requires integration publication.|No message contract tests required for deletion.|
|Cache|Do not add Redis/application cache for deletion.|No approved cache read model exists for notification inbox in this slice.|Deletion writes directly to the database; any existing query reads must observe database state.|Verification checks subsequent API/query absence after deletion.|
|Observability|Use structured application logs with requester ID, deletion scope, requested count, deleted count, and rejection reason category; no new tracing or metrics stack.|The repo already uses logging, and this slice only needs runtime diagnosability for API deletion outcomes.|Avoid logging notification content or secrets. Log IDs/counts at debug/info as appropriate and rejection categories without sensitive payloads.|Tests do not assert log lines unless existing project pattern already does; manual verification can inspect concise logs.|
|Validation|Use Bean Validation at DTO boundary for non-empty selected IDs and domain VO validation for `UserId`/`NotificationSelection` normalization.|Controller owns request shape; domain owns invariant-friendly value construction.|Keep duplicate removal and positive/non-null ID checks in `NotificationSelection` or boundary validation according to existing style. Application service receives normalized selection.|Controller tests cover invalid selected request rejection; domain tests cover selection normalization.|

## 4. Failure, Recovery, and Consistency Policy
|Situation|Policy|Retry/Compensation|Observability|Required Tests|
|---|---|---|---|---|
|Unauthenticated deletion request|Reject at gateway/security layer before application service.|No retry or compensation.|Existing auth failure logging/response handling.|Controller/gateway-facing test for unauthenticated rejection.|
|Single delete target missing|Reject without deleting any other notification.|No retry or compensation.|Log rejection category and notification ID.|Application test for missing single target.|
|Single delete target foreign-owned|Reject without deleting target.|No retry or compensation.|Log ownership rejection category, requester ID, notification ID.|Application test for foreign-owned single rejection.|
|Selected-set contains existing foreign-owned notification|Reject entire selected request and delete nothing.|No retry or compensation.|Log selected ownership rejection with requested count and requester ID.|Application test asserts owned requested rows remain after rejection.|
|Selected-set contains missing IDs|Ignore missing IDs after foreign-owned check over existing fetched rows and delete remaining existing owned notifications.|No retry or compensation.|Log requested count and deleted count.|Application test for partial selected deletion and zero remaining existing owned success.|
|All-owned deletion with no owned notifications|Succeed with zero deleted.|No retry or compensation.|Log all-owned delete count as zero.|Application test for empty all-owned scope.|
|Database transaction fails|Rollback transaction and surface failure through existing exception handling.|No application retry in this slice.|Error log through existing framework/service handling.|Integration test or repository-backed test verifies rollback where practical.|
|Concurrent selected deletion removes a requested row before this transaction deletes it|Treat as already missing for selected deletion if no foreign-owned existing row is found.|No lock or retry.|Deleted count may be lower than requested count.|No concurrency-specific test required unless implementation introduces explicit concurrency handling.|

## 5. Planner Requirements
- Decisions planner must include: `NotificationSelection` value object or equivalent normalization, `NotificationDeletionPolicy`, unscoped existing-ID fetch port for selected ownership rejection, hard-delete implementation, transaction-scoped service methods, and gateway-facing REST coverage for single, selected-set, and all-owned deletion.
- Decisions executor must not change: ChangeSet product behavior, DDD bounded contexts, gateway-first traffic rule, soft-delete/retention policy, external messaging/outbox behavior, or application-layer freedom from web types.
- Tests/verification planner must include: domain policy tests, application service tests for all UC-031 success/failure flows, repository/adapter tests for selected fetch/delete behavior, controller/auth tests for gateway-facing deletion, and an E2E/API verification that deleted owned notifications disappear from inbox while other recipients' notifications remain.

## 6. Slice-First External Lookup Record
|Outside Document|Why Read|Missing Slice Information|Conflict|Handling|
|---|---|---|---|---|
|notification source files|Confirm existing ports, services, controllers, and JPA adapter choices.|No missing slice policy; only implementation mechanism details.|Existing selected deletion uses recipient-scoped fetch, which cannot reject existing foreign-owned IDs.|Planner must adjust store port and selected policy implementation.|
|notification/AGENTS.md|Confirm module-local rules and verification command.|No missing slice policy.|No conflict.|Use `./gradlew :notification:test` for focused verification.|
|ARCHITECTURE.md|Required input.|No additional constraints present.|No conflict.|Rely on repo AGENTS and DDD integration contract for architecture constraints.|

## 7. Pending Decisions
- None
