# Purchase Module

Purpose: payment, purchase, refund, and purchase-domain consistency.

Look here:
- Domain: `purchase/src/main/java/org/codenbug/purchase/domain/`
- App layer: `purchase/src/main/java/org/codenbug/purchase/app/`
- UI: `purchase/src/main/java/org/codenbug/purchase/ui/`
- Infra: `purchase/src/main/java/org/codenbug/purchase/infra/`
- Tests: `purchase/src/test/java/`

Rules:
- Cross-service calls belong in `infra/client/`.
- State transitions need tests and explicit version/locking reasoning.
- Do not reuse test secrets in non-test config.

Command:
- `./gradlew :purchase:test`
