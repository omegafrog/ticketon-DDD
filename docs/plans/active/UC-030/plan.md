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
  - docs/changes/active/CHG-20260625-001.ddd-integration.json
  - ARCHITECTURE.md
  - .codex/repository-settings.md
status: active
work_item_id: UC-030
---
# Implementation Plan

## 1. 구현 목표
- 인증된 `Notification Recipient`가 gateway REST API를 통해 자신의 `Notification Inbox` 목록, `Unread Notification` 목록, `Unread Notification` 개수, `Notification` 상세를 조회할 수 있게 만든다.
- 목록, 미읽음 목록, 미읽음 개수는 recipient 범위와 latest-first pagination을 지키면서 상태를 바꾸지 않게 만든다.
- 상세 조회 성공 시 반환된 동일 `Notification` 한 건만 `UNREAD -> READ`로 전이하게 만든다.
- `UC-031`, `UC-032`와 공유하는 `NotificationAggregate` 계약을 깨지 않도록 ownership, read-state, query-view 규칙을 유지한다.

## 2. 구현하지 말아야 할 것
- `UC-031` 삭제 기능, `UC-032` 수동 생성 기능, SSE 구독, 신규 test 전용 발송 기능을 이번 work item 범위로 확장하지 않는다.
- `app/`에 business logic, controller, repository, listener를 추가하지 않는다.
- recipient 식별자를 client 입력으로 받지 않는다. 인증된 요청자 identity만 사용한다.
- cache, retry, circuit breaker, outbox/inbox, scheduler, retention, soft-delete state를 이번 slice에 도입하지 않는다.
- canonical 설계 문서와 completed plan 전이를 이 단계에서 수정하지 않는다.

## 3. 입력 문서
|문서|사용 목적|상태|
|---|---|---|
|`docs/changes/active/CHG-20260625-001.md`|ChangeSet 상태, 현재 절차, plan-writing 차단 원인 확인|present|
|`docs/use-cases/UC-030/use-case.md`|성공/실패 흐름과 관찰 가능한 계약 확인|present|
|`docs/use-cases/UC-030/event-storming.md`|명령, 이벤트, invariant, 인증 경계, 상태 전이 규칙 확인|present|
|`docs/use-cases/UC-030/ddd-design.md`|후보 서비스, 포트, aggregate 동작, bounded context 배치 확인|present|
|`docs/use-cases/UC-030/technical-decisions.md`|승인된 구현, 테스트, 검증 전략 확인|present, approved|
|`docs/use-cases/UC-030/e2e-goal.md`|API/runtime 관찰 목표 확인|present, approved|
|`docs/use-cases/UC-030/affected-files.md`|구현 허용 범위와 금지 경로 확인|present|
|`docs/changes/active/CHG-20260625-001.ddd-integration.md`|`UC-030`, `UC-031`, `UC-032` shared `NotificationAggregate` 정본 계약 확인|present, accepted|
|`docs/changes/active/CHG-20260625-001.ddd-integration.json`|정본 aggregate 상태, 명령, 이벤트, invariant 구조 확인|present|
|`notification/AGENTS.md`|notification 모듈 경계와 listener idempotency 규칙 확인|present|
|`ARCHITECTURE.md`|executor-facing 제약 존재 여부 확인|present, 내용 미기재|
|`.codex/repository-settings.md`|저장소 수준 검증 설정 확인|present, 세부 명령 미기재|
|`.codex/test-gate.yaml`|required test gate 확인|present, `required: []`|
|`docs/use-cases/UC-030/requirements-slice.md`|추가 요구사항 slice|missing optional|
|`docs/use-cases/UC-030/domain-impact.md`|도메인 영향 보강 자료|missing optional|
|`docs/use-cases/UC-030/aggregate-delta.md`|aggregate 변화 요약|missing optional|
|`docs/use-cases/UC-030/source-map.md`|코드-문서 매핑 보강|missing optional|

## 3.1 ChangeSet 및 Work Item
- ChangeSet: `CHG-20260625-001`
- Work item ID: `UC-030`
- Work item type: `use_case`
- Work item slice: `docs/use-cases/UC-030/`
- E2E/verification goal: 인증된 recipient가 gateway를 통해 자신의 inbox 목록, unread 목록, unread count, detail을 조회하고, detail 성공 시 해당 알림 한 건만 읽음 처리되는 것을 검증한다.

## 4. 아키텍처 제약
- `ARCHITECTURE.md` 기준:
  - 파일은 존재한다.
  - 현재 본문은 `# Architecture`와 빈 자리만 있어 실질 제약은 제공하지 않는다.
  - executor는 root `AGENTS.md`, `notification/AGENTS.md`, 승인된 `technical-decisions.md`, `ddd-integration.md`를 실질 제약으로 사용한다.
- 모듈/패키지 경계:
  - HTTP 진입과 인증 컨텍스트 추출은 `notification/.../controller`에 둔다.
  - 유스케이스 orchestration과 transaction 경계는 `notification/.../application/service`에 둔다.
  - ownership 검증과 read-state 전이는 `notification/.../domain`에 둔다.
  - JPA, Querydsl, messaging, listener adapter는 `notification/.../infrastructure` 또는 `notification/.../ui`에 둔다.
- 의존성 방향:
  - controller -> application service
  - application service -> domain + port
  - infrastructure/ui adapter -> Spring Data JPA, Querydsl, messaging
  - domain -> web, servlet, repository 구현체, messaging 구현체 의존 금지
- 금지 참조:
  - application service가 `NotificationStoreAdapter`, `NotificationRepository`, `NotificationViewRepositoryImpl` 같은 구현체를 직접 참조하지 않는다.
  - read repository에 상태 변경, 저장, 메시지 발행 부작용을 추가하지 않는다.
  - listener는 `notification/AGENTS.md` 규칙대로 idempotent하게 유지한다.

## 5. 구현 범위
- 포함:
  - `notification/src/main/java/org/codenbug/notification/controller/NotificationQueryController.java`
  - `notification/src/main/java/org/codenbug/notification/application/service/NotificationQueryService.java`
  - `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`
  - `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`
  - `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`
  - `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`
  - `notification/src/main/java/org/codenbug/notification/domain/service/NotificationDomainService.java`
  - `notification/src/main/java/org/codenbug/notification/infrastructure/NotificationStoreAdapter.java`
  - `notification/src/main/java/org/codenbug/notification/infrastructure/NotificationRepository.java`
  - `notification/src/main/java/org/codenbug/notification/ui/projection/NotificationListProjection.java`
  - `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepository.java`
  - `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepositoryImpl.java`
  - `notification/src/test/java/org/codenbug/notification/**`
  - `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`
- 제외:
  - `NotificationCommandController`, `TestNotificationController`, `NotificationEmitterService` 기능 확장
  - `UC-031`, `UC-032` 구현 자체
  - canonical 문서 동기화와 active -> completed plan 전이
- 가정:
  - gateway ingress 포트는 `8080`을 유지한다.
  - local runtime 검증은 유지 중인 launcher contract와 `harness run app`으로 수행한다.
  - 현재 dirty worktree 변경은 unrelated 변경일 수 있으므로 덮어쓰지 않고 `UC-030` 범위만 수정한다.

## 5.1 승인된 기술 결정
|영역|결정|구현 반영|테스트/검증 반영|
|---|---|---|---|
|Gateway entry|기존 Spring MVC REST endpoint를 `@AuthNeeded`, `@RoleRequired({Role.USER})`로 보호하고 요청자 identity는 `LoggedInUserContext`에서 가져온다.|controller는 recipient ID를 client에서 받지 않고 인증된 user ID만 service로 전달한다.|controller/security 테스트에서 보호 어노테이션과 인증된 user ID 전달을 검증한다.|
|Application boundary|`NotificationQueryService`가 inbox, unread, unread count, detail의 application service 경계다.|service 시그니처에 servlet/web 타입을 넣지 않고 `UserId`, `Pageable`, DTO/Projection만 사용한다.|service 테스트에서 read-only 경로와 detail write 경로를 분리해 검증한다.|
|Persistence|aggregate load/save는 JPA, 목록 조회는 Querydsl projection을 유지한다.|`NotificationStore`는 aggregate load/save/count, `NotificationViewRepository`는 recipient 범위 list/unread 조회를 담당한다.|repository 테스트에서 recipient filter, unread filter, latest-first, pagination을 검증한다.|
|Read state|정본 상태 집합은 `{UNREAD, READ}`만 허용한다.|기존 persisted 표현 위에서 같은 의미를 구현하되 추가 lifecycle state를 도입하지 않는다.|domain/service 테스트에서 성공한 owned detail만 읽음 전이되고 그 외 경로는 상태가 유지됨을 검증한다.|
|Transaction|list, unread list, unread count는 read-only, detail은 writable transaction이다.|detail은 `findById -> ownership check -> detail 반환 -> unread일 때만 markAsRead/save` 순서를 지킨다.|service 테스트에서 list/count는 save 미호출, unread owned detail만 save 1회 호출을 검증한다.|
|Concurrency|중복 detail 조회는 `READ`로 수렴하는 idempotent 동작이면 충분하다.|이미 읽은 알림은 추가 mutation 없이 반환되게 유지한다.|already-read detail 조회가 불필요한 save를 만들지 않는지 검증한다.|
|Caching/Retry/Messaging|cache, retry, circuit breaker, outbox/inbox는 도입하지 않는다.|동기 DB 접근과 단일 트랜잭션만 사용한다.|검증은 API, service, repository 결과 중심으로 수행한다.|
|Observability|새 metrics, tracing, 브로커 관찰성은 추가하지 않는다.|예외 로그가 필요하면 식별자만 남기고 content payload는 남기지 않는다.|검증 증거는 로그가 아니라 API, 테스트, 런처 결과로 남긴다.|

## 5.2 도메인 영향
|type|id|mode|canonical path|plan impact|
|---|---|---|---|---|
|Aggregate|`NotificationAggregate`|reuse+modify|`docs/changes/active/CHG-20260625-001.ddd-integration.md`|recipient-scoped query view와 단건 `UNREAD -> READ` 전이 계약을 유지한다.|
|Entity|`Notification`|modify|`docs/changes/active/CHG-20260625-001.ddd-integration.md`|상세 성공 시 동일 root 한 건만 읽음 전이되도록 aggregate behavior를 점검한다.|
|Value Object|`UserId`|reuse|`docs/changes/active/CHG-20260625-001.ddd-integration.md`|요청자 scope와 ownership 검증을 같은 식별자 개념으로 유지한다.|
|Value Object|`NotificationContent`|reuse|`docs/changes/active/CHG-20260625-001.ddd-integration.md`|목록과 상세 응답의 payload shape를 깨지 않게 유지한다.|
|Value Object|`NotificationReadState`|modify|`docs/changes/active/CHG-20260625-001.ddd-integration.md`|정본 의미는 `{UNREAD, READ}`만 허용한다. 다른 상태를 추가하지 않는다.|
|Port|`NotificationStore`|modify|`docs/use-cases/UC-030/ddd-design.md`|aggregate load/save/count는 port 뒤에 유지한다.|
|Read Adapter|`NotificationViewRepository`|modify|`docs/use-cases/UC-030/ddd-design.md`|latest-first, recipient-only, unread-only 조회 semantics를 유지하고 부작용을 추가하지 않는다.|

## 5.3 호환성 확인
- 기존 유스케이스 영향:
  - `UC-031`은 같은 `NotificationAggregate`의 ownership 규칙을 공유하므로 `UC-030` detail 처리와 ownership 판단이 삭제 규칙을 깨지 않아야 한다.
  - `UC-032`는 unread 생성 계약을 공유하므로 생성된 알림이 `UC-030` inbox, unread list, unread count에서 그대로 관찰되어야 한다.
  - listener 기반 persisted notification 생성 경로는 `UC-030` 조회와 계속 호환되어야 한다.
- 같은 도메인 요소를 수정하는 active ChangeSet 충돌 여부:
  - `docs/changes/active`에서 추가 active ChangeSet 문서는 보이지 않았다.
  - 다만 현재 worktree에 `notification` 관련 dirty 변경이 이미 있으므로 executor는 unrelated 변경을 보존하면서 범위 안 수정만 해야 한다.

## 5.4 OWASP Security Review
- Status: plan 반영 완료, 별도 대기 게이트 없음
- Attack surface:
  - gateway REST 조회 endpoint의 인증/권한 경계
  - `notificationId`, `page`, `size` 입력
  - foreign-owned detail 접근 시도
  - detail 성공 후 단건 read-state 전이
- Applicable standards:
  - OWASP ASVS access control
  - OWASP ASVS input validation and error handling
  - OWASP API Security Top 10의 object-level authorization, resource consumption
- Exclusions and rationale:
  - 브라우저 UI, CSRF, 파일 업로드, 외부 callback, 신규 암호화 설계는 이번 slice 범위 밖이다.

## 6. 구현 계획
- [x] `spring-initializer`가 불필요함을 확인한다. 이번 work item은 신규 Spring Boot baseline이나 신규 모듈 추가가 아니다.
- [x] `spring-package-structure` 기준으로 `notification` 모듈의 controller, application, domain, infrastructure, ui 경계와 `app` orchestration-only 규칙을 현재 구조와 대조해 위반 여부를 먼저 기록한다.
- [x] `git status --porcelain=v1 -uno`와 현재 dirty 파일을 확인하고 `UC-030` 범위 밖 변경을 덮어쓰지 않도록 작업 경계를 고정한다.
- [x] `NotificationQueryController`의 inbox, unread, unread count, detail endpoint가 인증된 `USER`만 허용하고 recipient ID를 `LoggedInUserContext`에서만 가져오도록 유지 또는 보완한다.
- [x] `NotificationQueryController`의 pageable 처리에서 latest-first ordering, 양수 기본 size, 최대 size 제한, caller sort 무시 규칙을 유지 또는 보완한다.
- [x] `NotificationQueryService`의 list, unread list, unread count 메서드를 read-only 경로로 유지하고 save 또는 상태 전이가 일어나지 않도록 정리한다.
- [x] `NotificationQueryService` detail 경로를 `findById -> ownership 확인 -> detail 반환 -> unread일 때만 markAsRead/save` 순서로 유지 또는 보완한다.
- [x] `Notification` aggregate와 read-state 표현을 정리해 `UNREAD -> READ` 단건 전이만 허용하고 미존재, 타인 소유, 이미 읽음 경로에서 불필요한 mutation이 없게 한다.
- [x] `NotificationStore`, `NotificationStoreAdapter`, `NotificationRepository`를 점검해 application layer가 port 뒤에서만 aggregate 조회, 저장, unread count를 수행하게 유지한다.
- [x] `NotificationViewRepository`, `NotificationViewRepositoryImpl`, `NotificationListProjection`을 점검해 recipient-only, unread-only, latest-first pagination 조회 semantics를 유지하고 read repository에 부작용을 추가하지 않는다.
- [x] listener와 생성 호환 경로를 확인해 persisted notification이 `UC-030` inbox 조회에서 계속 관찰 가능하고 listener idempotency 규칙을 깨지 않게 유지한다.
- [x] `notification` 범위 테스트를 추가 또는 수정해 controller, service, domain, repository, listener, compatibility 시나리오를 `UC-030` 계약 기준으로 고정한다.
- [x] E2E 재현 절차를 고정한다: 두 개의 `USER` 계정을 register/login하고 `/api/v1/users/me`로 실제 `userId`를 확보한 뒤, MySQL에 owned/unread 및 foreign/unread notification fixture를 삽입하고 SQL 조회로 `OWNED_NOTIFICATION_ID`, `FOREIGN_NOTIFICATION_ID`를 확보한다.
- [x] `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh` 런처 계약이 현재 구현과 맞는지 확인하고 필요 시 `harness run app` 검증이 가능하도록 갱신한다.
- [x] 구현 후 검증 결과를 이 plan의 `## 10. 검증 결과`와 `## 11. 검증 실패`에 기록한다.

## 7. 테스트 계획
- [x] Domain/Aggregate/VO 테스트:
  - owned unread detail 조회만 `READ`로 전이되는지 검증한다.
  - already-read detail 조회가 추가 save 없이 반환되는지 검증한다.
  - ownership 규칙이 foreign-owned detail 접근을 거절하는지 검증한다.
- [x] Application Service 흐름 테스트:
  - inbox, unread list, unread count가 recipient scope만 반영하고 save를 호출하지 않는지 검증한다.
  - detail 조회가 owned notification만 읽음 처리하고 missing, foreign-owned detail은 실패시키는지 검증한다.
- [x] Infrastructure/Adapter 테스트:
  - `NotificationStoreAdapter`, `NotificationRepository`가 aggregate 조회, 저장, unread count 계약을 충족하는지 검증한다.
  - `NotificationViewRepositoryImpl`가 latest-first pagination, recipient filter, unread filter를 지키는지 검증한다.
- [x] Communication/Transaction 테스트:
  - detail transaction이 unread owned detail에서만 save 1회를 수행하는지 검증한다.
  - listener 기반으로 저장된 notification이 query path와 호환되는지 확인한다.
- [x] Compatibility 테스트:
  - `UC-031`, `UC-032`가 공유하는 `NotificationAggregate` shape와 ownership, read-state 규칙이 깨지지 않는지 확인한다.
  - 생성된 unread notification이 `UC-030` inbox, unread list, unread count에서 계속 관찰 가능한지 확인한다.

## 8. 검증 방법
- [ ] Build:
  - 명령: `./gradlew build --no-daemon --console=plain`
  - 성공 기준: multi-module build가 성공하고 `notification` 범위 변경으로 인한 compile, test, package 오류가 없다.
- [x] Tests:
  - 명령: `./gradlew :notification:test --no-daemon --console=plain`
  - 명령: `./gradlew test --no-daemon --console=plain`
  - 성공 기준: notification 집중 테스트와 전체 회귀 테스트가 통과한다. 범위 밖 기존 실패가 있으면 원인과 영향 범위를 plan에 기록한다.
- [ ] E2E 또는 maintenance verification:
  - 명령:
    - `python3 -m harness_codex run app status`
    - `python3 -m harness_codex run app --foreground`
    - `scripts/run-app-infra.sh`
    - `scripts/check-app-infra.sh`
    - `scripts/run-app-server.sh`
    - `USER_A_EMAIL="uc030-user-a+$(date +%s)@ticketon.local"; USER_B_EMAIL="uc030-user-b+$(date +%s)@ticketon.local"; USER_PASSWORD="Ticketon123!"`
    - `curl -sS -X POST http://127.0.0.1:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"email\":\"${USER_A_EMAIL}\",\"password\":\"${USER_PASSWORD}\",\"name\":\"UC030 User A\",\"age\":29,\"sex\":\"ETC\",\"phoneNum\":\"010-3000-0001\",\"location\":\"Seoul\"}"`
    - `curl -sS -X POST http://127.0.0.1:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"email\":\"${USER_B_EMAIL}\",\"password\":\"${USER_PASSWORD}\",\"name\":\"UC030 User B\",\"age\":31,\"sex\":\"ETC\",\"phoneNum\":\"010-3000-0002\",\"location\":\"Busan\"}"`
    - `USER_A_TOKEN="$(curl -sS -X POST http://127.0.0.1:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"email\":\"${USER_A_EMAIL}\",\"password\":\"${USER_PASSWORD}\"}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"])')"`
    - `USER_B_TOKEN="$(curl -sS -X POST http://127.0.0.1:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"email\":\"${USER_B_EMAIL}\",\"password\":\"${USER_PASSWORD}\"}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"])')"`
    - `until USER_A_ID="$(curl -sS -H "Authorization: Bearer ${USER_A_TOKEN}" http://127.0.0.1:8080/api/v1/users/me | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["userId"])' 2>/dev/null)"; do sleep 2; done`
    - `until USER_B_ID="$(curl -sS -H "Authorization: Bearer ${USER_B_TOKEN}" http://127.0.0.1:8080/api/v1/users/me | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["userId"])' 2>/dev/null)"; do sleep 2; done`
    - `docker exec -i mysql-master mysql -uroot -ppassword ticketon -e "insert into notification (user_id, type, title, content, target_url, sent_at, is_read, status, source_key) values ('${USER_A_ID}', 'SYSTEM', 'UC030 owned unread', 'owned unread notification', '/notifications/owned', now() - interval 2 minute, false, 'SENT', 'uc030-owned-${USER_A_ID}'), ('${USER_B_ID}', 'SYSTEM', 'UC030 foreign unread', 'foreign unread notification', '/notifications/foreign', now() - interval 1 minute, false, 'SENT', 'uc030-foreign-${USER_B_ID}');"`
    - `OWNED_NOTIFICATION_ID="$(docker exec -i mysql-master mysql -N -uroot -ppassword ticketon -e "select id from notification where source_key = 'uc030-owned-${USER_A_ID}'")"`
    - `FOREIGN_NOTIFICATION_ID="$(docker exec -i mysql-master mysql -N -uroot -ppassword ticketon -e "select id from notification where source_key = 'uc030-foreign-${USER_B_ID}'")"`
    - `curl -i http://127.0.0.1:8080/api/v1/notifications`
    - `curl -i -H "Authorization: Bearer ${USER_A_TOKEN}" "http://127.0.0.1:8080/api/v1/notifications?page=0&size=10"`
    - `curl -i -H "Authorization: Bearer ${USER_A_TOKEN}" "http://127.0.0.1:8080/api/v1/notifications/unread?page=0&size=10"`
    - `curl -i -H "Authorization: Bearer ${USER_A_TOKEN}" "http://127.0.0.1:8080/api/v1/notifications/count/unread"`
    - `curl -i -H "Authorization: Bearer ${USER_A_TOKEN}" "http://127.0.0.1:8080/api/v1/notifications/${OWNED_NOTIFICATION_ID}"`
    - `curl -i -H "Authorization: Bearer ${USER_A_TOKEN}" "http://127.0.0.1:8080/api/v1/notifications/${FOREIGN_NOTIFICATION_ID}"`
    - `docker exec -i mysql-master mysql -N -uroot -ppassword ticketon -e "select id, user_id, is_read, title from notification where id in (${OWNED_NOTIFICATION_ID}, ${FOREIGN_NOTIFICATION_ID}) order by id"`
  - 목표: gateway를 통해 recipient-scoped list, unread list, unread count, detail과 detail 후 단건 read-state 전이를 재현 가능한 fixture 위에서 관찰한다.
  - 성공 기준: 비인증 요청은 거절되고, 인증된 `USER_A` 요청은 `USER_A_ID` 소유 알림만 반환하며, `OWNED_NOTIFICATION_ID` 상세 성공 후 해당 row만 `is_read=true`로 바뀌고 `FOREIGN_NOTIFICATION_ID` 상세는 거절되며 foreign row 상태는 그대로 유지된다.
- [x] Test gate:
  - 기준: `.codex/test-gate.yaml`의 `required` stage 확인
  - 성공 기준: 현재 `required: []`이므로 추가 강제 gate가 없음을 검증 결과에 기록한다.
- [ ] Runtime server verification:
  - 서버 실행 명령:
    - `python3 -m harness_codex run app --foreground`
    - 내부 런처 계약: `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`
  - 구현사항 확인 방법:
    - gateway `8080`이 기동되는지 확인한다.
    - 인증, 비인증 inbox API 호출로 recipient scope, latest-first, unread count, detail read transition을 확인한다.
  - 성공 기준: launcher contract로 앱을 기동할 수 있고 runtime surface가 `UC-030` E2E goal을 만족한다.
- [x] Static analysis:
  - 절차: 저장소에 이미 존재하는 ArchUnit, Semgrep 기반 DDD 규칙을 `notification` 변경 범위에 적용한다.
  - 명령:
    - `./gradlew architectureRules --no-daemon --console=plain`
    - `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java`
  - 성공 기준: 계층, 의존성 위반이 없고 blocking finding이 0건이다. 예외가 있으면 근거와 범위를 기록한다.

## 9. 완료 조건
- 모든 체크박스가 `- [x]` 상태다.
- 구현 범위의 테스트가 작성되어 통과했다.
- Build, Tests, E2E 또는 maintenance verification, Test gate, Runtime server verification, Static analysis가 성공했다.
- 검증 결과가 기록되어 있다.
- active -> completed 전이는 workflow의 `complete-work-item-plan` git step이 수행한다.

## 10. 검증 결과
- Build: 실행됨. `./gradlew build --no-daemon --console=plain`은 `nplus1-test/src/test/java/org/codenbug/nplus1/EventManagerEventsNoNPlusOneTest.java:123`의 범위 밖 기존 compile failure로 실패했다.
- Tests: 실행됨. `./gradlew :notification:test --no-daemon --console=plain`은 통과했고, `./gradlew test --no-daemon --console=plain`은 동일한 `nplus1-test` 범위 밖 compile failure로 실패했다.
- E2E 또는 maintenance verification: Docker CLI가 현재 WSL distro에 없어 compose-backed infra와 gateway runtime 확인이 차단됐다.
- Test gate: 확인됨. `.codex/test-gate.yaml`의 현재 `required: []`로 추가 강제 gate가 없다.
- Runtime server verification: Docker CLI 부재로 `python3 -m harness_codex run app --foreground -- --debug-jvm` 실행 경로가 차단됐다.
- Static analysis: 실행됨. `./gradlew architectureRules --no-daemon --console=plain`은 통과했고, `notification` 범위 Semgrep은 blocking finding 없이 통과했다. repo-wide Semgrep에서는 범위 밖 `purchase` finding이 관찰됐다.

## 11. 검증 실패
- `./gradlew build --no-daemon --console=plain`: 범위 밖 `nplus1-test` compile failure.
- `./gradlew test --no-daemon --console=plain`: 동일한 범위 밖 `nplus1-test` compile failure.
- Runtime/E2E: 현재 WSL distro에 Docker CLI가 없어 compose-backed infra와 gateway runtime 확인이 차단됨.
- Repo-wide Semgrep: 범위 밖 `purchase` module finding 2건. `UC-030` notification 범위 static analysis는 통과.
