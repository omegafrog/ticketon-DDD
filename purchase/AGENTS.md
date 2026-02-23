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


## Runtime Verification Policy (Agent)
- When validating "server starts normally" or runtime behavior, prefer JDB MCP verification over log-only checks.
- Default flow:
  1) Start target service in debug mode (`--debug-jvm`) so JDWP opens on port `5005`.
  2) Attach via JDB MCP (or `jdb -attach 5005` as fallback) and verify thread/session visibility.
  3) Confirm service readiness via HTTP status or health endpoint.
  4) Stop the service unless persistence is explicitly requested.
- Keep verification evidence in temporary logs (for example under `/tmp`) and summarize key lines in the response.
- Do not expose secrets while collecting runtime evidence.

## Pull Request Writing Policy (Agent)
- When creating a PR, write a detailed body by default.
- Include `## Summary`, `## Changes`, `## Verification`, and `## Risks and Rollback` sections.
- Explain scope boundaries (what is included and excluded) so reviewers can quickly assess impact.
- Link follow-up tasks or TODOs when work is intentionally deferred.