# User Module

Purpose: user profile, registration-related flows, and validation endpoints.

Look here:
- UI: `user/src/main/java/org/codenbug/user/ui/`
- Domain: `user/src/main/java/org/codenbug/user/domain/`
- App layer: `user/src/main/java/org/codenbug/user/app/`
- Infra: `user/src/main/java/org/codenbug/user/infra/`
- Validation API: `user/src/main/java/org/codenbug/user/ui/UserValidationController.java`

Rules:
- Messaging consumers live under `infra/consumer/`.
- Keep consumer side effects out of domain entities.
- Validation endpoints must not depend on UI-layer state.

Command:
- `./gradlew :user:test`
