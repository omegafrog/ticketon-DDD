# 구현 계획

## 1. 구현 목표
- ChangeSet: `CHG-20260625-001`
- Work item: `UC-031` (`use-case`)
- 목표: 인증된 `Notification Recipient`가 gateway 경유 API로 자신의 `Notification`을 단건, `selected-set`, `all-owned` 범위로 삭제하고, `selected-set`에 existing foreign-owned `Notification`이 포함되면 전체 거절되며 이미 없는 owned ID는 무시되도록 구현한다.

## 2. 구현하지 말아야 할 것
- `UC-030` 조회/읽음 전이, `UC-032` 생성 규칙 자체를 재설계하지 않는다.
- gateway-first 진입 구조, 기존 security AOP 책임, role 정책을 이번 work item에서 재정의하지 않는다.
- soft delete 상태, cache 무효화 체계, outbox/inbox, 비동기 삭제 이벤트를 추가하지 않는다.
- 통합 설계 문서, canonical domain 문서, completed plan 경로를 수정하지 않는다.

## 3. 입력 문서
- Slice: `docs/use-cases/UC-031/use-case.md`, `docs/use-cases/UC-031/event-storming.md`, `docs/use-cases/UC-031/ddd-design.md`, `docs/use-cases/UC-031/technical-decisions.md`, `docs/use-cases/UC-031/e2e-goal.md`, `docs/use-cases/UC-031/affected-files.md`
- E2E/verification goal: 인증된 요청의 단건, `selected-set`, `all-owned` 삭제 성공과 비인증/foreign-owned 포함 요청 거절, 삭제 후 inbox 부재를 gateway API로 확인한다.
- 필수 입력: `docs/changes/active/CHG-20260625-001.md`, `docs/changes/active/CHG-20260625-001.ddd-integration.md`, `docs/changes/active/CHG-20260625-001.ddd-integration.json`, `ARCHITECTURE.md`, `.codex/repository-settings.md` 포함 필수 입력 모두 존재한다.
- 누락/placeholder: `ARCHITECTURE.md`와 `.codex/repository-settings.md`는 상세 제약/명령이 미기입 상태다. `requirements-slice.md`, `domain-impact.md`, `aggregate-delta.md`, `source-map.md`는 현재 slice에 없다.

## 4. 아키텍처 제약
- 경계/의존성: client traffic은 `platform/gateway` `8080`으로 진입한다. controller는 adapter-only로 유지하고 삭제 정책은 domain/application에 둔다. `app/`은 orchestration-only를 유지한다. service domain/app layer에 web type을 넣지 않는다. 삭제는 aggregate 제거이며 soft-delete 상태를 두지 않는다.
- 기술 결정: 삭제 명령은 Spring `@Transactional` 경계 안에서 처리한다. `selected-set`은 owner-agnostic existing lookup 후 domain policy로 foreign-owned 거절과 missing-owned 무시를 수행한다. outbox, retry, circuit breaker, cache는 추가하지 않는다.
- 도메인 영향: `NotificationAggregate` 변경은 `docs/changes/active/CHG-20260625-001.ddd-integration.md`, `docs/changes/active/CHG-20260625-001.ddd-integration.json`, `docs/use-cases/UC-031/ddd-design.md`를 기준으로 맞춘다. 호환성 테스트는 `UC-030`, `UC-032` 공유 aggregate 규칙을 함께 확인한다.
- 도메인 영향: `NotificationSelection`은 `selected-set` 삭제 전용 VO로 반영하고 중복 제거 집합 semantics를 유지한다. 호환성 테스트는 foreign-owned 포함 거절, missing-owned 무시, 0건 정상 종료를 확인한다.
- 도메인 영향: `NotificationDeletionPolicy`는 foreign-owned 포함 거절과 existing owned 필터링 규칙만 소유한다. 호환성 테스트는 단건 ownership 검증과 `selected-set` 집합 정책이 함께 유지되는지 확인한다.
- 충돌/호환성: `docs/changes/active/CHG-20260625-001.ddd-integration.md` 기준 blocked conflict는 없다. 다만 `UC-030`, `UC-032`가 같은 `NotificationAggregate`를 공유하므로 aggregate shape, ownership, unread 생성/조회 호환성 회귀 테스트가 필요하다.
- OWASP Security Review: pending `security_plan_reviewer`; attack surface: 인증된 삭제 endpoint, path/body `notificationId` 입력, foreign-owned 거절 응답, count/scope 로그. 토큰, secret config, 과도한 payload 로그 금지.

## 5. 구현 범위
- 포함: `NotificationSelection`/`NotificationDeletionPolicy` 반영, `NotificationStore` selected lookup 계약 확장, `NotificationCommandService` 삭제 흐름 정렬, controller/DTO adapter-only 검토, notification 테스트 보강, gateway runtime 검증, launcher/static-analysis 점검.
- 제외: 조회 pagination 규칙 재설계, 생성 이벤트 흐름 변경, 새로운 인프라 의존성 추가, canonical docs 동기화, completed plan 전이.
- 위험/가정: 현재 `ARCHITECTURE.md`와 repository settings가 상세하지 않으므로 실제 구조 판단은 기존 모듈 경계와 approved technical decisions에 의존한다. `NotificationDeleteRequestDto`의 empty selected-set 검증은 승인된 UC 범위 밖 정책이므로 새 user-visible 정책을 확장하지 않도록 주의한다. 기존 dirty worktree는 건드리지 않는다.

## 6. 구현 계획
- [ ] `spring-initializer`가 불필요함을 확인한다. 이번 work item은 신규 Spring Boot baseline이나 신규 모듈 추가가 아니다.
- [ ] `spring-package-structure` 기준으로 `notification` 모듈의 controller/application/domain/infrastructure 경계와 `app` orchestration-only 규칙을 대조하고, 삭제 로직이 adapter 밖으로 유지되도록 작업 경계를 고정한다.
- [ ] `git status --porcelain=v1 -uno` 기준 dirty tree를 다시 확인하고 `UC-031` 범위 파일만 수정하도록 보호한다.
- [ ] `NotificationSelection` value object와 `NotificationDeletionPolicy` domain 규칙을 추가 또는 정렬해 중복 ID 정규화, foreign-owned 포함 시 전체 거절, missing owned 제외, 남은 existing owned 0건 정상 종료를 표현한다.
- [ ] `NotificationStore`, `NotificationStoreAdapter`, `NotificationRepository`에 requested ID 전체를 owner와 무관하게 읽는 selected-set lookup 계약을 추가 또는 보완하고, requester-owned 전체 조회 계약은 유지한다.
- [ ] `NotificationCommandService`의 단건, `selected-set`, `all-owned` 삭제 흐름을 `find -> ownership/policy 검증 -> delete` 순서로 정렬하고, soft delete나 controller 비즈니스 로직 없이 transaction 안에서 마무리한다.
- [ ] `NotificationDeleteRequestDto`와 `NotificationCommandController`를 adapter-only로 유지하고, empty selected-set에 대한 새 product 정책을 도입하지 않도록 입력 매핑과 검증 범위를 최소 조정한다.
- [ ] `UC-030`, `UC-032` 공유 `NotificationAggregate` 영향 지점을 점검해 조회/생성 경로와의 호환성을 깨지 않도록 관련 contract를 맞춘다.
- [ ] `notification` 범위 domain/application/adapter 테스트와 필요한 compatibility 테스트를 추가 또는 수정한다.
- [ ] `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh`가 현재 gateway 삭제 검증에 맞는지 확인하고 필요 시만 갱신한다.
- [ ] 구현 후 실제 명령 결과와 범위 밖 기존 실패 여부를 `## 10. 검증 결과`에 기록한다.

## 7. 테스트 계획
- [ ] 단위/도메인: `NotificationSelection` 중복 정규화, `NotificationDeletionPolicy` owned-only/foreign-owned/missing-owned/zero-existing 규칙, 단건 ownership 거절을 검증한다.
- [ ] 애플리케이션/어댑터: `NotificationCommandService`의 single/selected/all delete 흐름, `NotificationStore` selected lookup 계약, controller 인증 경계와 adapter-only 매핑을 검증한다.
- [ ] 호환성/E2E: `UC-030` inbox 조회에서 삭제 후 부재가 관찰되는지, `UC-032` 생성 알림이 삭제 흐름과 충돌하지 않는지, foreign-owned 포함 `selected-set`이 partial delete 없이 거절되는지 검증한다.

## 8. 검증 방법
- [ ] Build: `./gradlew :notification:build --no-daemon --console=plain` -> `notification` 모듈 compile/package가 성공하고 변경 범위로 인한 build 오류가 없다.
- [ ] Tests: `./gradlew :notification:test --no-daemon --console=plain`, `./gradlew test --no-daemon --console=plain` -> notification 집중 테스트가 통과하고 전체 회귀 테스트도 통과하거나, 범위 밖 기존 실패면 원인과 영향 범위를 기록한다.
- [ ] E2E 또는 maintenance verification: `python3 -m harness_codex run app status`, `python3 -m harness_codex run app --foreground`, `curl -i -X DELETE http://127.0.0.1:8080/api/v1/notifications/<OWNED_ID>`, `curl -i -X POST -H "Content-Type: application/json" -H "Authorization: Bearer <USER_TOKEN>" -d '{"notificationIds":[<OWNED_ID>,<FOREIGN_OR_MISSING_ID>]}' http://127.0.0.1:8080/api/v1/notifications/batch-delete`, `curl -i -X DELETE -H "Authorization: Bearer <USER_TOKEN>" http://127.0.0.1:8080/api/v1/notifications/all`, `curl -i -H "Authorization: Bearer <USER_TOKEN>" "http://127.0.0.1:8080/api/v1/notifications?page=0&size=10"` -> 세 삭제 범위 성공, 비인증 거절, foreign-owned 포함 요청 거절, 삭제 후 inbox 부재가 gateway 응답으로 확인된다.
- [ ] Test gate: `.codex/test-gate.yaml`의 `required: []` 확인 -> 추가 강제 stage가 없음을 검증 결과에 기록한다.
- [ ] Runtime server verification: `python3 -m harness_codex run app --foreground`와 `scripts/run-app-infra.sh`, `scripts/check-app-infra.sh`, `scripts/run-app-server.sh` -> launcher contract로 gateway `8080`이 기동되고 삭제/query API가 응답한다.
- [ ] Static analysis: `./gradlew architectureRules --no-daemon --console=plain`, `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java` -> DDD 경계 위반과 blocking finding이 0건이다.

## 9. 완료 조건
- 모든 체크박스가 `- [x]`.
- 필요한 테스트가 존재하고 통과.
- Build, Tests, E2E 또는 maintenance verification, Test gate, Runtime server verification, Static analysis 결과 기록.
- active -> completed 전이는 `complete-work-item-plan`만 수행.

## 10. 검증 결과
- Build: 미실행
- Tests: 미실행
- E2E 또는 maintenance verification: 미실행
- Test gate: 미실행
- Runtime server verification: 미실행
- Static analysis: 미실행
