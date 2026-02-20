# Purchase Service

Purchase owns payment/purchase flows and related domain consistency (locking/versioning).

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Domain | `purchase/src/main/java/org/codenbug/purchase/domain/` | Purchase/ticket domain |
| App layer | `purchase/src/main/java/org/codenbug/purchase/app/` | Use-cases |
| UI | `purchase/src/main/java/org/codenbug/purchase/ui/` | Controllers/requests |
| Infra | `purchase/src/main/java/org/codenbug/purchase/infra/` | Clients, messaging, persistence |
| Tests | `purchase/src/test/java/` | Uses `PurchaseTestApplication` harness |
| Docs | `docs/purchase/` | Payment flow + plans |
| Config | `purchase/src/main/resources/application.yml` | Secrets in test resources too |

## Common Endpoints
| Area | Location | Notes |
|------|----------|------|
| Purchase commands | `purchase/src/main/java/org/codenbug/purchase/ui/PurchaseController.java` | Primary write API |
| Purchase queries | `purchase/src/main/java/org/codenbug/purchase/ui/PurchaseQueryController.java` | Read APIs |
| Refund | `purchase/src/main/java/org/codenbug/purchase/ui/RefundController.java` | Refund flow |

## Conventions
- Cross-service calls belong in `infra/client/`.
- Be careful changing purchase state transitions; prefer tests and explicit version checks.

## Anti-Patterns
- Writing to other services directly from controllers.
- Reusing test secrets from `src/test/resources/application-secret.yml` in non-test configs.

## Commands
```bash
./gradlew :purchase:test
```
