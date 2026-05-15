# Platform Message

Purpose: cross-service message/event contracts for messaging and integration.

Look here:
- Message types: `platform/message/src/main/java/org/codenbug/message/`
- RabbitMQ notes: `docs/platform/message/rabbitmq.md`

Rules:
- Treat DTOs as published contracts.
- Prefer additive fields over breaking rename/removal.
- Check consumers before changing package/name/field semantics.
- Do not use message DTOs as internal domain entities.

Command:
- `./gradlew :platform:message:test`
