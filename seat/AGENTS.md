# Seat Module

Purpose: seat layouts, availability, and internal endpoints used by purchase/event flows.

Look here:
- UI: `seat/src/main/java/org/codenbug/seat/ui/`
- Domain: `seat/src/main/java/org/codenbug/seat/domain/`
- App layer: `seat/src/main/java/org/codenbug/seat/app/`
- Infra: `seat/src/main/java/org/codenbug/seat/infra/`
- Config: `seat/src/main/resources/application.yml`

Rules:
- Service clients belong in `infra/`.
- Do not leak external API DTOs into domain.
- Do not perform remote calls from domain objects.

Command:
- `./gradlew :seat:test`
