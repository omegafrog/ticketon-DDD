# UC-030. Technical Decisions

## 1. Metadata
|Item|Value|
|---|---|
|ChangeSet|CHG-20260625-001|
|Use Case|UC-030|
|Approval Status|approved|
|Approved By|runtime technical-decisions stage; content review feedback was empty and no blocking mechanism choices remain|

## 2. Input Documents
|Document|Status|Use|
|---|---|---|
|docs/changes/active/CHG-20260625-001.md|read|ChangeSet scope and active runtime state.|
|docs/use-cases/UC-030/use-case.md|read|Inbox, unread, count, detail, and read-transition behavior.|
|docs/use-cases/UC-030/event-storming.md|read|Commands, events, invariants, and gateway authentication boundary.|
|docs/use-cases/UC-030/ddd-design.md|read|Application service, ports, aggregate, transaction, and query-view candidates.|
|docs/use-cases/UC-030/e2e-goal.md|read|Runtime-observable acceptance evidence and failure criteria.|
|docs/changes/active/CHG-20260625-001.ddd-integration.md|read|Accepted canonical aggregate, state model, and ChangeSet-wide invariants.|
|docs/changes/active/CHG-20260625-001.ddd-integration.json|read|Machine-readable accepted model coverage and invariants.|
|ARCHITECTURE.md|read|Architecture baseline placeholder; implementation evidence came from module code and build files.|
|notification/AGENTS.md|read|Notification module boundaries and verification command.|

## 3. Approved Decisions
|Decision Area|Decision|Rationale|Implementation Impact|Test/Verification Impact|
|---|---|---|---|---|
|Runtime entrypoint|Serve UC-030 through existing Spring MVC REST endpoints in `notification` behind gateway-first traffic.|Use case requires gateway REST API and repo already routes client traffic through `platform/gateway`; notification module already exposes `/api/v1/notifications` controllers.|Keep web concerns in controller only. Extract authenticated user ID at controller boundary and pass primitives or `UserId` into application service.|Controller tests must verify `@AuthNeeded`, role guard, pageable normalization, and service user scoping.|
|Authentication adapter|Use existing `security-aop` annotations and `LoggedInUserContext` at controller boundary as service-side enforcement.|Event storming says unauthenticated requests are blocked before notification query handling; existing controller stack already uses `@AuthNeeded` and `@RoleRequired`.|Do not pass servlet request/response into application/domain. Application service receives requester identity as input.|Tests must assert protected query endpoints and service calls use authenticated requester user ID.|
|Application service|Use `NotificationQueryService` as UC-030 application service with read-only default transaction and write transaction only for detail read transition.|DDD design places inbox, unread, count, and detail/read transition in one application service. Existing service already matches this shape.|Keep list, unread list, and count read-only. Annotate detail retrieval with `@Transactional` so detail response construction and `UNREAD -> READ` save happen in one unit of work.|Service tests must cover no state change for list/unread/count and exactly one save for unread detail retrieval.|
|Persistence adapter|Use Spring Data JPA `NotificationRepository` behind `NotificationStore` for aggregate loading, counting, and saving.|Existing module depends on Spring Data JPA and has a port/adapter split. DDD integration accepts persisted `NotificationAggregate`.|Application services depend on `NotificationStore`, not `NotificationRepository`. Keep repository in infrastructure.|Port tests must ensure application services do not depend directly on infrastructure repository types.|
|List query adapter|Use QueryDSL-backed `NotificationViewRepository` projections for inbox and unread list pages.|UC-030 list responses are read views. Existing `NotificationViewRepositoryImpl` already projects recipient-scoped latest-first pages without mutating aggregate state.|Keep projection repository side-effect-free. Force sort to `sentAt DESC` through controller/service path.|Repository tests must verify recipient filter, unread filter, latest-first order, pagination, and no write side effects.|
|Unread count|Use `NotificationStore.countByUserIdAndIsReadFalse(UserId)` for unread count.|Unread count is scalar aggregate state query and must not mutate read state.|Do not derive count from a fetched page. Query count directly through persistence port.|Service tests must verify count returns store count and does not save.|
|Read-state representation|Implement canonical `NotificationReadState` semantics on current persisted `isRead` column unless planner deliberately introduces a small VO wrapper around the same persisted value.|DDD integration accepts `{UNREAD, READ}` and existing storage uses boolean `isRead`. No new storage technology is needed for this slice.|Planner may refactor domain method names to expose read-state language, but schema can remain boolean-compatible.|Domain tests must verify unread/read semantics and idempotent read marking.|
|Detail consistency|Load notification by ID, validate ownership, return detail, then mark same aggregate read within the same transaction when unread.|Use case requires failed non-existent or non-owned detail not to change state and successful detail retrieval to change only that notification.|Ownership validation must happen before `markAsRead`. Save only after validated success and only for unread notification.|Tests must cover non-existent, non-owned, already-read, and unread-owned detail paths.|
|Concurrency/idempotency|Treat concurrent detail reads as idempotent: multiple successful reads may observe same notification, final state remains READ.|UC-030 requires final read transition for same single notification and does not require exactly-once event emission or version conflict behavior.|No distributed lock, Redis lock, queue, outbox, or optimistic-lock conflict policy is required for this read transition.|Tests should assert repeated detail retrieval leaves notification read and does not affect other notifications.|
|Messaging/outbox|Do not add outbox, inbox, queue, RabbitMQ consumer, or domain event publication for UC-030 query/read transition.|UC-030 has no external system collaboration and only synchronous REST-observable behavior.|Keep messaging adapters out of this slice except existing unrelated listeners.|No messaging contract or container test required for UC-030.|
|Caching|Do not add Redis or application cache for inbox, unread, detail, or count.|Read-after-detail correctness matters because detail retrieval changes unread state. Cache invalidation would add risk without accepted need.|Every UC-030 query reads current persistence state.|Verification must perform count/list before and after detail retrieval to catch stale data.|
|Observability|Use existing Spring Boot logging, Micrometer/Actuator stack where present; add focused logs only for rejected detail lookup/ownership if implementation needs diagnostics.|No new observability platform is needed. Existing modules already include actuator/prometheus in runtime modules.|Avoid logging sensitive token or request payload data. Log IDs/user IDs only if existing logging policy permits.|Tests focus on behavior; no new metrics assertion required unless planner adds metrics.|
|Error handling|Map unauthenticated rejection through existing security/gateway handling; map missing or non-owned detail to existing application/controller exception handling without state mutation.|Failure flows require rejection, not a new business recovery process.|Do not add compensation. Do not mutate aggregate in failure branches.|Controller/service tests must verify failure branches do not call save.|

## 4. Failure, Recovery, and Consistency Policy
|Situation|Policy|Retry/Compensation|Observability|Required Tests|
|---|---|---|---|---|
|Unauthenticated inbox, unread, count, or detail request|Reject at gateway/security AOP before application query handling.|No retry or compensation in notification module.|Existing security/gateway logs and HTTP response handling.|Controller/security annotation tests for all UC-030 endpoints.|
|List query persistence failure|Fail request; transaction is read-only and no state changes occur.|Client may retry; no server compensation.|Existing exception handling and application logs.|Repository/service failure test optional; no mutation assertion required.|
|Unread list/count query persistence failure|Fail request; read state remains unchanged.|Client may retry; no compensation.|Existing exception handling and application logs.|Tests verifying normal unread list/count do not save or mark read.|
|Detail target does not exist|Reject request before ownership/read transition.|No retry policy beyond client retry with valid ID; no compensation.|Existing exception handling.|Service test: missing ID throws/rejects and does not save.|
|Detail target belongs to another recipient|Reject request after load and ownership validation, before read transition.|No compensation.|Existing exception handling; optional warn/debug without secrets.|Service test: foreign-owned notification rejects and does not save.|
|Detail target already READ|Return detail without additional state change.|No retry or compensation.|No special observability required.|Service test: already-read detail returns and does not call save unless implementation intentionally persists no-op.|
|Detail target UNREAD and owned|Return detail and persist same aggregate as READ in one transaction.|Transaction rollback restores unread if save fails.|Existing transaction rollback and logs.|Service/integration test: only retrieved notification becomes read; other notifications remain unchanged.|
|Concurrent detail requests for same notification|Allow idempotent final READ state.|No lock or retry required for this slice.|Existing persistence logs if failure occurs.|Repeated detail retrieval test; optional transactional integration test.|

## 5. Planner Requirements
- Decisions planner must include: REST controller route coverage for inbox list, unread list, unread count, and detail; controller extraction of authenticated requester ID; `NotificationQueryService` methods matching UC-030; `NotificationStore` and `NotificationViewRepository` adapter use; `sentAt DESC` pageable normalization with bounded page size; detail read transition inside a write transaction.
- Decisions planner must include: persistence/query tests for recipient scoping, unread filter, count, latest-first pagination, missing detail, foreign-owned detail, already-read detail, unread-owned detail, and repeated detail retrieval.
- Decisions executor must not change: gateway-first traffic model, module boundaries, application/domain independence from servlet/web types, notification application services depending on ports instead of repositories, side-effect-free projection repositories, no new messaging/cache/lock infrastructure for UC-030.
- Tests/verification planner must include: `./gradlew :notification:test` and focused controller/application/repository tests named in this document; broader architecture tests if implementation changes module dependencies.

## 6. Slice-First External Lookup Record
|Outside Document|Why Read|Missing Slice Information|Conflict|Handling|
|---|---|---|---|---|
|notification/AGENTS.md|Needed module-specific boundaries and verification command.|None.|None.|Applied notification module rules.|
|notification/build.gradle|Needed existing framework/library choices.|None.|None.|Kept Spring MVC, JPA, validation, AMQP dependency baseline; did not add new libraries.|
|notification application/controller/repository source files|Needed existing adapter, transaction, and persistence evidence because `ARCHITECTURE.md` is placeholder.|None.|None.|Aligned decisions with current port/adapter and QueryDSL/JPA implementation style.|

## 7. Pending Decisions
- None
