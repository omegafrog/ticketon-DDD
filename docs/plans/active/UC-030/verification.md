# UC-030 Verification Notes

## Implemented Contract
- Inbox pagination contract:
  - maximum page size: `100`
  - accepted effective sort: `sentAt DESC`
  - hostile or unsupported caller `sort` input is normalized to `sentAt DESC`
- Detail endpoint now requires `@AuthNeeded` and `@RoleRequired(Role.USER)`.
- Query endpoints now route through `NotificationQueryService` instead of controller-direct repository access.
- Detail reads stay ownership-checked and save only unread notifications.
- Listener idempotency uses persisted `source_key`:
  - payment: `payment.completed:userId:purchaseId:approvedAt`
  - refund: `refund.completed:userId:purchaseId:refundedAt`
  - notification refund: `notification.refund.completed:userId:purchaseId:refundedAt`
  - manager refund: `notification.manager.refund.completed:userId:purchaseId:refundedAt:managerName`
- `scripts/run-app-server.sh` now forwards extra runtime args into `:app:bootRun`, so `harness run app -- --debug-jvm` reaches app startup path.

## Focused Test Coverage
- `notification/src/test/java/org/codenbug/notification/controller/NotificationQueryControllerTest.java`
- `notification/src/test/java/org/codenbug/notification/application/service/NotificationQueryServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/application/service/NotificationCommandServiceIdempotencyTest.java`
- `notification/src/test/java/org/codenbug/notification/infrastructure/event/PurchaseEventListenerTest.java`
- `notification/src/test/java/org/codenbug/notification/infrastructure/messaging/PurchaseNotificationEventListenerTest.java`
- `notification/src/test/java/org/codenbug/notification/ui/repository/NotificationViewRepositoryImplTest.java`
- `notification/src/test/java/org/codenbug/notification/domain/service/NotificationDomainServiceTest.java`

## Observed Results
- `./gradlew :notification:test --no-daemon --console=plain`:
  - PASS
  - Query controller normalization/security-contract tests passed.
  - Query service recipient scope, missing/foreign detail, unread/detail read-state tests passed.
  - Domain service ownership and unread-to-read guard tests passed.
  - Listener idempotency/malformed-payload tests passed.
  - Read-model newest-first/unread filtering test passed.
- `./gradlew build --no-daemon --console=plain`:
  - FAIL outside UC-030 scope.
  - Existing `nplus1-test` compile failure persists: `EventCategory()` protected constructor access in `nplus1-test/src/test/java/org/codenbug/nplus1/EventManagerEventsNoNPlusOneTest.java:123`.
- `./gradlew test --no-daemon --console=plain`:
  - FAIL outside UC-030 scope for same `nplus1-test` compile error.
- `./gradlew architectureRules --no-daemon --console=plain`:
  - PASS
- `TMPDIR=/tmp HOME=/tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml notification/src/main/java notification/src/test/java`:
  - PASS
  - No notification-scope DDD boundary findings.
- `python3 -m harness_codex run app status`:
  - PASS
  - No existing tmux app sessions were left running.
- `python3 -m harness_codex run app --foreground -- --debug-jvm`:
  - BLOCKED by environment.
  - Docker CLI unavailable in current WSL distro, so compose-backed infra and gateway/app runtime checks could not start.
- Reconfirmed on `2026-06-19T18:16:55+09:00`:
  - `./gradlew build --no-daemon --console=plain` still fails only in `:nplus1-test` at `EventManagerEventsNoNPlusOneTest.java:123`.
  - `./gradlew test --no-daemon --console=plain` still fails only in `:nplus1-test` at same line.
  - `python3 -m harness_codex run app status` reported `infra: missing`, `server: missing`.
  - `python3 -m harness_codex run app --foreground -- --debug-jvm` still exits immediately with `The command 'docker' could not be found in this WSL 2 distro.`
