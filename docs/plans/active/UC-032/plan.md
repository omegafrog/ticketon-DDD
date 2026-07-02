---
change_set_id: CHG-20260625-001
contract_version: 1
doc_id: "UC-032:plan"
doc_type: plan
source_docs:
  - docs/changes/active/CHG-20260625-001.md
  - docs/use-cases/UC-032/use-case.md
  - docs/use-cases/UC-032/event-storming.md
  - docs/use-cases/UC-032/ddd-design.md
  - docs/changes/active/CHG-20260625-001.ddd-integration.md
  - docs/changes/active/CHG-20260625-001.ddd-integration.json
  - docs/use-cases/UC-032/technical-decisions.md
  - docs/use-cases/UC-032/affected-files.md
  - docs/use-cases/UC-032/e2e-goal.md
  - ARCHITECTURE.md
  - .codex/repository-settings.md
status: active
work_item_id: UC-032
---
# 구현 계획

## 1. 구현 목표
- ChangeSet: `CHG-20260625-001`
- Work item: `UC-032` (`use_case`)
- 목표: 인증된 `ADMIN` 또는 `MANAGER`가 gateway 경유 API로 지정한 `Recipient User ID` 대상 unread `Notification` 1건을 생성하고, 생성 직후 recipient-scoped inbox 조회 경로에서 관찰 가능하게 만든다.

## 2. 구현하지 말아야 할 것
- `UC-030` 조회/읽음 전이, `UC-031` 삭제 규칙 자체를 재설계하지 않는다.
- `app/`에 business logic, controller, repository를 추가하지 않는다.
- direct create 경로에 outbox, retry, cache, scheduler, 신규 inbox entity, broker 의존 성공 조건을 추가하지 않는다.
- domain/application 계층 메서드 시그니처에 servlet/web 타입을 넣지 않는다.
- 통합 설계 문서, canonical domain 문서, completed plan 경로를 수정하지 않는다.

## 3. 입력 문서
- Slice: `docs/use-cases/UC-032/use-case.md`, `docs/use-cases/UC-032/event-storming.md`, `docs/use-cases/UC-032/ddd-design.md`, `docs/use-cases/UC-032/technical-decisions.md`, `docs/use-cases/UC-032/e2e-goal.md`, `docs/use-cases/UC-032/affected-files.md`
- E2E/verification goal: 인증된 `ADMIN`/`MANAGER` 생성 성공, `USER`/비인증/필수 입력 실패, 생성 후 recipient inbox 가시성을 gateway API로 확인한다.
- 필수 입력: `docs/changes/active/CHG-20260625-001.md`, `docs/changes/active/CHG-20260625-001.ddd-integration.md`, `docs/changes/active/CHG-20260625-001.ddd-integration.json`, `ARCHITECTURE.md`, `.codex/repository-settings.md`, `.codex/test-gate.yaml` 모두 존재한다.
- 누락/placeholder: `ARCHITECTURE.md`는 실질 제약이 비어 있다. `.codex/repository-settings.md`는 테스트 명령 상세가 없다. `requirements-slice.md`, `domain-impact.md`, `aggregate-delta.md`, `source-map.md`는 현재 slice에 없다. `affected-files.md`의 `NotificationCreateRequestDto` 경로 표기는 실제 코드 위치와 다르므로 executor가 실제 패키지 기준으로 작업해야 한다.

## 4. 아키텍처 제약
- 경계/의존성: client traffic은 `platform/gateway` `8080`으로 진입한다. create endpoint는 `notification` web adapter에 두고, 트랜잭션 orchestration은 `NotificationCommandService`, 생성 규칙과 ownership/value validation은 `domain`에 둔다. persistence는 `NotificationStore` port 뒤 JPA adapter만 사용한다.
- 기술 결정: create endpoint는 기존 Spring MVC `NotificationCommandController` 아래 `/api/v1/notifications`를 유지한다. `@AuthNeeded`와 `@RoleRequired({Role.ADMIN, Role.MANAGER})`로 access control을 선행 적용한다. `NotificationCommandService.createNotification(...)`가 단일 `@Transactional` 경계로 validate -> create -> save -> local event publish -> DTO 반환을 수행한다.
- 도메인 영향: `NotificationAggregate`, `Notification`, `UserId`, `NotificationContent`, `NotificationReadState`, `NotificationDomainService`, `NotificationStore` 계약은 `docs/changes/active/CHG-20260625-001.ddd-integration.md`와 `docs/use-cases/UC-032/ddd-design.md`를 따른다. 호환성 테스트는 `UC-030` inbox/unread 조회와 `UC-031` ownership/delete 규칙이 유지되는지 함께 확인해야 한다.
- 충돌/호환성: accepted integration 문서 기준 blocked conflict는 없다. 다만 worktree에 `notification/**` 기존 dirty 변경이 있으므로 executor는 그 변경을 덮어쓰지 않고 create slice 범위만 조정해야 한다.
- OWASP Security Review: pending `security_plan_reviewer`; attack surface: authenticated create endpoint, `Recipient User ID`/`type`/`title`/`content`/`targetUrl` request body, role bypass 시도, invalid payload 저장 금지, generated notification visibility 확인.

## 5. 구현 범위
- 포함: `NotificationCommandController`, 실제 위치의 `NotificationCreateRequestDto`, `NotificationCommandService`, `NotificationStore`, `Notification`, `NotificationContent`, `UserId`, `NotificationDomainService`, `NotificationStoreAdapter`, `NotificationRepository`, listener/create compatibility 영향 지점, `notification` 테스트, `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`
- 제외: `app/**`, `platform/**`, `auth/**`, `purchase/**`, `seat/**`, `event/**`, `dispatcher/**`, `broker/**`, `user/**`, 신규 persisted inbox 모델, async delivery 보장 설계, canonical docs sync, active -> completed 전이
- 위험/가정: 현재 코드상 `NotificationContent`는 `content == null`을 허용하는 흔적이 있어 UC-032 정본 계약과 어긋날 수 있다. create endpoint는 recipient를 request body에서 받는 계약이라 authenticated sender identity를 recipient로 오인하면 안 된다. runtime 검증에는 유효한 `ADMIN`/`MANAGER` 토큰과 recipient fixture가 필요하다.

## 6. 구현 계획
- [ ] `spring-initializer`가 불필요함을 확인한다. 이번 work item은 신규 Spring Boot baseline이나 신규 모듈 추가가 아니다.
- [ ] `spring-package-structure` 기준으로 `notification` 모듈의 controller/application/domain/infrastructure 경계와 `app` orchestration-only 규칙을 대조하고 create 흐름이 adapter -> service -> domain -> port 순서를 유지하는지 먼저 고정한다.
- [ ] `git status --porcelain=v1 -uno` 기준 dirty tree를 다시 확인하고 `UC-032` 범위 파일만 수정하도록 작업 경계를 고정한다.
- [ ] `NotificationCommandController`의 create endpoint가 `/api/v1/notifications` POST, `@AuthNeeded`, `@RoleRequired({Role.ADMIN, Role.MANAGER})`, `@Valid` request body, `201` 응답 계약을 정확히 유지하는지 확인하고 필요한 최소 수정만 한다.
- [ ] 실제 `NotificationCreateRequestDto` 위치를 기준으로 `Recipient User ID`, `type`, `title`, `content` required와 `targetUrl` optional 계약을 검증 annotation 또는 명시적 validation으로 고정한다.
- [ ] `NotificationContent`와 `NotificationDomainService.createNotification(...)`를 정리해 blank recipient ID, missing/blank title, missing/blank content가 save 전에 거절되고 optional `targetUrl`만 허용되게 만든다.
- [ ] `NotificationCommandService.createNotification(...)`가 `NotificationStore` port 뒤에서 정확히 1건 save, unread 초기 상태 유지, DTO 매핑, local event publish를 수행하고 invalid path에서 save가 일어나지 않도록 정리한다.
- [ ] `Notification` aggregate 생성 결과가 canonical `NotificationReadState.UNREAD` 의미와 현재 persisted `isRead=false` 표현을 일관되게 유지하는지 확인하고, create 경로에서 다른 상태 초기화를 허용하지 않게 만든다.
- [ ] `NotificationStoreAdapter`/`NotificationRepository`와 query path를 점검해 생성 직후 recipient-scoped inbox 또는 unread-inbox 조회에서 새 알림이 관찰되는지 확인한다. 별도 inbox row나 projection write는 추가하지 않는다.
- [ ] `PurchaseEventListener`와 `PurchaseNotificationEventListener` 같은 기존 생성 경로가 stricter content validation 이후에도 깨지지 않는지 확인하고 필요 시 compatibility 보완만 한다.
- [ ] `notification` 범위 테스트를 추가 또는 수정해 role 경계, validation 실패 no-save, optional target URL, unread 초기 상태, inbox visibility, shared aggregate compatibility를 고정한다.
- [ ] `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`가 현재 create/query 검증 흐름과 맞는지 확인하고 필요 시만 갱신한다.
- [ ] 구현 후 실제 명령 결과와 범위 밖 기존 실패 여부를 `## 10. 검증 결과`에 기록한다.

## 7. 테스트 계획
- [ ] 단위/도메인: `UserId`/`NotificationContent`/`NotificationDomainService`가 blank recipient, blank title, blank content를 거절하고 optional `targetUrl`, unread 초기 상태를 허용하는지 검증한다.
- [ ] 애플리케이션/어댑터: `NotificationCommandService`가 valid create에서 save 1회와 event publish 1회를 수행하고 invalid input에서는 save 0회인지 검증한다. create controller가 `ADMIN`/`MANAGER` 허용, `USER`/비인증 거절, request validation 실패를 올바르게 매핑하는지 검증한다.
- [ ] 호환성/E2E: 생성된 unread notification이 `UC-030` inbox 또는 unread query에서 관찰되는지, stricter create validation이 purchase/listener 기반 생성 경로와 충돌하지 않는지, `UC-031` ownership/delete 규칙과 aggregate shape가 유지되는지 검증한다.

## 8. 검증 방법
- [ ] Build: `./gradlew :notification:build --no-daemon --console=plain` -> `notification` 모듈 compile/package가 성공하고 변경 범위 build 오류가 없다.
- [ ] Tests: `./gradlew :notification:test --no-daemon --console=plain`, `./gradlew test --no-daemon --console=plain` -> notification 집중 테스트가 통과하고 전체 회귀 테스트도 통과하거나, 범위 밖 기존 실패면 원인과 영향 범위를 기록한다.
- [ ] E2E 또는 maintenance verification: `python3 -m harness_codex run app status`, `python3 -m harness_codex run app --foreground`, `curl -i -X POST -H "Content-Type: application/json" -H "Authorization: Bearer <ADMIN_OR_MANAGER_TOKEN>" -d '{"userId":"<RECIPIENT_USER_ID>","type":"SYSTEM","title":"운영 공지","content":"생성 검증","targetUrl":"/notifications"}' http://127.0.0.1:8080/api/v1/notifications`, `curl -i -X POST -H "Content-Type: application/json" -H "Authorization: Bearer <USER_TOKEN>" -d '{"userId":"<RECIPIENT_USER_ID>","type":"SYSTEM","title":"차단","content":"차단"}' http://127.0.0.1:8080/api/v1/notifications`, `curl -i -X POST -H "Content-Type: application/json" -d '{"userId":"<RECIPIENT_USER_ID>","type":"SYSTEM","title":"비인증","content":"비인증"}' http://127.0.0.1:8080/api/v1/notifications`, `curl -i -X POST -H "Content-Type: application/json" -H "Authorization: Bearer <ADMIN_OR_MANAGER_TOKEN>" -d '{"userId":" ","type":"SYSTEM","title":"실패","content":""}' http://127.0.0.1:8080/api/v1/notifications`, `curl -i -H "Authorization: Bearer <RECIPIENT_TOKEN>" "http://127.0.0.1:8080/api/v1/notifications?page=0&size=10"`, `curl -i -H "Authorization: Bearer <RECIPIENT_TOKEN>" "http://127.0.0.1:8080/api/v1/notifications/unread?page=0&size=10"` -> create 성공, 실패 거절, 생성 후 inbox 가시성이 gateway 응답으로 확인된다.
- [ ] Test gate: `.codex/test-gate.yaml`의 `required: []` 확인 -> 추가 강제 stage가 없음을 검증 결과에 기록한다.
- [ ] Runtime server verification: `python3 -m harness_codex run app --foreground`와 `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh` -> launcher contract로 gateway `8080`이 기동되고 create/query API가 응답한다.
- [ ] Static analysis: `./gradlew architectureRules --no-daemon --console=plain`, `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java` -> DDD 경계 위반과 blocking finding이 0건이다.

## 9. 완료 조건
- 모든 체크박스가 `- [x]`.
- 필요한 테스트가 존재하고 통과.
- Build, Tests, E2E 또는 maintenance verification, Test gate, Runtime server verification, Static analysis 결과 기록.
- active -> completed 전이는 `complete-work-item-plan`만 수행한다.

## 10. 검증 결과
- Build: 미실행
- Tests: 미실행
- E2E 또는 maintenance verification: 미실행
- Test gate: 미실행
- Runtime server verification: 미실행
- Static analysis: 미실행
