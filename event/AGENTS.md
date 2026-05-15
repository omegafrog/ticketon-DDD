# Event Module

Purpose: event commands, event queries, and projections.

Look here:
- UI: `event/src/main/java/org/codenbug/event/ui/`
- Domain: `event/src/main/java/org/codenbug/event/domain/`
- App layer: `event/src/main/java/org/codenbug/event/application/`
- Query/projections: `event/src/main/java/org/codenbug/event/query/`
- Infra: `event/src/main/java/org/codenbug/event/infra/`

Rules:
- Keep controller DTOs in `ui/`.
- Keep persistence adapters in `infra/`.
- Do not put projection logic into domain entities.

Command:
- `./gradlew :event:test`
