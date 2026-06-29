# UC-030. 기술 결정

## 1. 메타데이터
|항목|값|
|---|---|
|ChangeSet|CHG-20260625-001|
|Use Case|UC-030|
|Approval Status|approved|
|승인 근거|runtime technical-decisions stage에서 확정했고, content review feedback이 비어 있으며 남은 차단 기술 선택지가 없다.|

## 2. 입력 문서
|문서|상태|사용 목적|
|---|---|---|
|docs/changes/active/CHG-20260625-001.md|읽음|ChangeSet 범위와 active runtime 상태 확인.|
|docs/use-cases/UC-030/use-case.md|읽음|Inbox, unread, count, detail, read-transition 동작 확인.|
|docs/use-cases/UC-030/event-storming.md|읽음|Command, event, invariant, gateway 인증 경계 확인.|
|docs/use-cases/UC-030/ddd-design.md|읽음|Application service, port, aggregate, transaction, query-view 후보 확인.|
|docs/use-cases/UC-030/e2e-goal.md|읽음|런타임에서 관찰 가능한 acceptance evidence와 실패 기준 확인.|
|docs/changes/active/CHG-20260625-001.ddd-integration.md|읽음|승인된 canonical aggregate, state model, ChangeSet-wide invariant 확인.|
|docs/changes/active/CHG-20260625-001.ddd-integration.json|읽음|기계 판독 가능한 승인 모델 coverage와 invariant 확인.|
|ARCHITECTURE.md|읽음|Architecture baseline placeholder 확인. 구현 근거는 module code와 build file에서 보강했다.|
|notification/AGENTS.md|읽음|Notification module 경계와 검증 명령 확인.|

## 3. 승인된 결정
|결정 영역|결정|근거|구현 영향|테스트/검증 영향|
|---|---|---|---|---|
|Runtime entrypoint|UC-030은 gateway-first traffic 뒤의 `notification` 기존 Spring MVC REST endpoint로 제공한다.|Use case는 gateway REST API를 요구하고, repo는 이미 `platform/gateway`를 통해 client traffic을 라우팅한다. `notification` 모듈도 이미 `/api/v1/notifications` controller를 가진다.|Web concern은 controller에만 둔다. 인증된 user ID는 controller boundary에서 추출하고 application service에는 primitive 또는 `UserId`로 전달한다.|Controller test는 `@AuthNeeded`, role guard, pageable normalization, service user scoping을 검증해야 한다.|
|Authentication adapter|Controller boundary에서 기존 `security-aop` annotation과 `LoggedInUserContext`를 사용해 service-side enforcement를 수행한다.|Event storming은 unauthenticated request가 notification query handling 전에 차단된다고 말한다. 기존 controller stack도 `@AuthNeeded`, `@RoleRequired`를 사용한다.|Servlet request/response를 application/domain으로 넘기지 않는다. Application service는 requester identity를 입력으로 받는다.|Test는 보호된 query endpoint와 service call이 인증된 requester user ID를 쓰는지 검증해야 한다.|
|Application service|`NotificationQueryService`를 UC-030 application service로 사용한다. list/count 경로는 read-only transaction이고 detail read transition만 write transaction을 사용한다.|DDD design은 inbox, unread, count, detail/read transition을 하나의 application service에 둔다. 기존 service도 이 형태와 맞는다.|List, unread list, count는 read-only로 유지한다. Detail retrieval에는 `@Transactional`을 적용해 detail response 생성과 `UNREAD -> READ` save가 하나의 unit of work 안에서 끝나게 한다.|Service test는 list/unread/count에서 state change가 없고 unread detail retrieval에서 save가 정확히 한 번 일어나는지 검증해야 한다.|
|Persistence adapter|Aggregate loading, counting, saving은 `NotificationStore` 뒤의 Spring Data JPA `NotificationRepository`로 수행한다.|기존 모듈은 Spring Data JPA에 의존하고 port/adapter split을 가진다. DDD integration은 persisted `NotificationAggregate`를 승인한다.|Application service는 `NotificationRepository`가 아니라 `NotificationStore`에 의존한다. Repository는 infra layer에 둔다.|Port test는 application service가 infrastructure repository type에 직접 의존하지 않는지 확인해야 한다.|
|List query adapter|Inbox와 unread list page는 QueryDSL 기반 `NotificationViewRepository` projection을 사용한다.|UC-030 list response는 read view다. 기존 `NotificationViewRepositoryImpl`은 recipient-scoped latest-first page를 aggregate state mutation 없이 projection한다.|Projection repository는 side-effect-free로 유지한다. Controller/service path에서 sort를 `sentAt DESC`로 강제한다.|Repository test는 recipient filter, unread filter, latest-first order, pagination, write side effect 없음 여부를 검증해야 한다.|
|Unread count|Unread count는 `NotificationStore.countByUserIdAndIsReadFalse(UserId)`를 사용한다.|Unread count는 scalar aggregate state query이며 read state를 변경하면 안 된다.|Fetched page에서 count를 유도하지 않는다. Persistence port를 통해 count를 직접 조회한다.|Service test는 count가 store count를 반환하고 save를 호출하지 않는지 검증해야 한다.|
|Read-state representation|Canonical `NotificationReadState` 의미는 현재 persisted `isRead` column 위에 구현한다. Planner가 필요하다고 판단하면 같은 persisted value를 감싸는 작은 VO wrapper만 도입할 수 있다.|DDD integration은 `{UNREAD, READ}`를 승인했고 기존 storage는 boolean `isRead`를 사용한다. 이 slice에는 새 storage technology가 필요 없다.|Planner는 read-state language를 드러내도록 domain method name을 refactor할 수 있지만 schema는 boolean-compatible로 유지할 수 있다.|Domain test는 unread/read 의미와 idempotent read marking을 검증해야 한다.|
|Detail consistency|Notification을 ID로 load하고, ownership을 검증하고, detail을 반환한 뒤, unread일 때 같은 transaction 안에서 같은 aggregate를 read로 표시한다.|Use case는 non-existent 또는 non-owned detail 실패가 state를 바꾸지 않고, successful detail retrieval이 해당 notification만 바꾸기를 요구한다.|Ownership validation은 `markAsRead`보다 먼저 수행해야 한다. Save는 validated success 이후, unread notification에 대해서만 수행한다.|Test는 non-existent, non-owned, already-read, unread-owned detail path를 모두 다뤄야 한다.|
|Concurrency/idempotency|동시 detail read는 idempotent하게 처리한다. 여러 successful read가 같은 notification을 관찰할 수 있고 final state는 READ로 남는다.|UC-030은 동일 single notification의 최종 read transition을 요구하며 exactly-once event emission이나 version conflict behavior를 요구하지 않는다.|이 read transition에는 distributed lock, Redis lock, queue, outbox, optimistic-lock conflict policy가 필요 없다.|Test는 repeated detail retrieval 이후 notification이 read 상태이고 다른 notification에는 영향이 없음을 확인해야 한다.|
|Messaging/outbox|UC-030 query/read transition에는 outbox, inbox, queue, RabbitMQ consumer, domain event publication을 추가하지 않는다.|UC-030은 external system collaboration이 없고 synchronous REST-observable behavior만 가진다.|기존 unrelated listener를 제외하고 messaging adapter를 이 slice에 끌어들이지 않는다.|UC-030에는 messaging contract test나 container test가 필요 없다.|
|Caching|Inbox, unread, detail, count에 Redis 또는 application cache를 추가하지 않는다.|Detail retrieval이 unread state를 바꾸므로 read-after-detail correctness가 중요하다. Cache invalidation은 승인된 필요 없이 위험만 늘린다.|모든 UC-030 query는 현재 persistence state를 읽는다.|Verification은 detail retrieval 전후 count/list를 수행해 stale data를 잡아야 한다.|
|Observability|기존 Spring Boot logging, Micrometer/Actuator stack이 있으면 그대로 사용한다. 구현상 진단이 필요할 때만 rejected detail lookup/ownership에 focused log를 추가한다.|새 observability platform은 필요 없다. 기존 모듈은 actuator/prometheus를 runtime module에 이미 포함한다.|Sensitive token이나 request payload data를 logging하지 않는다. 기존 logging policy가 허용할 때만 ID/user ID를 기록한다.|Test는 behavior에 집중한다. Planner가 metric을 추가하지 않는 한 새 metric assertion은 필요 없다.|
|Error handling|Unauthenticated rejection은 기존 security/gateway handling에 맡긴다. Missing 또는 non-owned detail은 state mutation 없이 기존 application/controller exception handling으로 mapping한다.|Failure flow는 recovery process가 아니라 rejection을 요구한다.|Compensation을 추가하지 않는다. Failure branch에서 aggregate를 변경하지 않는다.|Controller/service test는 failure branch가 save를 호출하지 않는지 검증해야 한다.|

## 4. 실패, 복구, 일관성 정책
|상황|정책|재시도/보상|관측성|필수 테스트|
|---|---|---|---|---|
|Unauthenticated inbox, unread, count, detail request|Application query handling 전에 gateway/security AOP에서 거절한다.|Notification module 안에서는 retry 또는 compensation이 없다.|기존 security/gateway log와 HTTP response handling을 사용한다.|모든 UC-030 endpoint에 대한 controller/security annotation test.|
|List query persistence failure|Request를 실패시킨다. Transaction은 read-only이고 state change는 없다.|Client는 retry할 수 있다. Server compensation은 없다.|기존 exception handling과 application log를 사용한다.|Repository/service failure test는 optional이다. Mutation 없음 assertion은 필요하다.|
|Unread list/count query persistence failure|Request를 실패시키고 read state는 변경하지 않는다.|Client retry 외 compensation은 없다.|기존 exception handling과 application log를 사용한다.|정상 unread list/count가 save 또는 mark read를 하지 않는지 검증한다.|
|Detail target does not exist|Ownership/read transition 전에 request를 거절한다.|Valid ID로 client retry하는 것 외 별도 retry policy나 compensation은 없다.|기존 exception handling을 사용한다.|Service test: missing ID는 throw/reject되고 save하지 않는다.|
|Detail target belongs to another recipient|Load와 ownership validation 후, read transition 전에 request를 거절한다.|Compensation 없음.|기존 exception handling을 사용한다. Secret 없이 warn/debug log를 둘 수 있다.|Service test: foreign-owned notification은 reject되고 save하지 않는다.|
|Detail target already READ|추가 state change 없이 detail을 반환한다.|Retry 또는 compensation 없음.|특별 관측성 필요 없음.|Service test: already-read detail은 반환되고, 구현이 intentional no-op persist를 하지 않는 한 save하지 않는다.|
|Detail target UNREAD and owned|Detail을 반환하고 같은 aggregate를 한 transaction 안에서 READ로 persist한다.|Save 실패 시 transaction rollback으로 unread가 복원된다.|기존 transaction rollback과 log를 사용한다.|Service/integration test: 조회한 notification만 read가 되고 다른 notification은 그대로 남는다.|
|Concurrent detail requests for same notification|Idempotent final READ state를 허용한다.|이 slice에는 lock 또는 retry가 필요 없다.|실패 시 기존 persistence log를 사용한다.|Repeated detail retrieval test. Optional transactional integration test.|

## 5. 계획 작성 요구사항
- 계획 작성자가 포함해야 할 결정: inbox list, unread list, unread count, detail에 대한 REST controller route coverage, controller에서 authenticated requester ID 추출, UC-030에 맞는 `NotificationQueryService` method, `NotificationStore`와 `NotificationViewRepository` adapter 사용, bounded page size와 `sentAt DESC` pageable normalization, write transaction 안의 detail read transition.
- 계획 작성자가 포함해야 할 결정: recipient scoping, unread filter, count, latest-first pagination, missing detail, foreign-owned detail, already-read detail, unread-owned detail, repeated detail retrieval에 대한 persistence/query test.
- 구현 실행자가 변경하면 안 되는 결정: gateway-first traffic model, module boundary, application/domain이 servlet/web type에 의존하지 않는 규칙, notification application service가 repository가 아니라 port에 의존하는 규칙, side-effect-free projection repository, UC-030에 messaging/cache/lock infrastructure를 추가하지 않는 결정.
- 테스트/검증 계획에 포함해야 할 항목: `./gradlew :notification:test`, 이 문서에 명시한 focused controller/application/repository test, module dependency를 바꾸는 경우 broader architecture test.

## 6. Slice 우선 외부 조회 기록
|외부 문서|조회 이유|Slice에 없던 정보|충돌|처리|
|---|---|---|---|---|
|notification/AGENTS.md|Module-specific boundary와 verification command가 필요했다.|없음.|없음.|Notification module rule을 적용했다.|
|notification/build.gradle|기존 framework/library 선택을 확인해야 했다.|없음.|없음.|Spring MVC, JPA, validation, AMQP dependency baseline을 유지했고 새 library를 추가하지 않았다.|
|notification application/controller/repository source files|`ARCHITECTURE.md`가 placeholder라 기존 adapter, transaction, persistence evidence가 필요했다.|없음.|없음.|현재 port/adapter와 QueryDSL/JPA implementation style에 맞춰 결정했다.|

## 7. 보류 중인 결정
- 없음
