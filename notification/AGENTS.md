# Notification Module

Purpose: consumes domain events and provides notification APIs/emitters.

Look here:
- UI/API: `notification/src/main/java/org/codenbug/notification/ui/`
- Application: `notification/src/main/java/org/codenbug/notification/application/`
- Messaging/persistence: `notification/src/main/java/org/codenbug/notification/infrastructure/`
- Domain: `notification/src/main/java/org/codenbug/notification/domain/`
- Emitter: `notification/src/main/java/org/codenbug/notification/service/NotificationEmitterService.java`

Rules:
- Keep event adapters in `infrastructure/`.
- Keep listeners idempotent.
- Avoid side effects in projection/read repositories.

Command:
- `./gradlew :notification:test`
