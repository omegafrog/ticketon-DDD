# Documentation (docs/)

`docs/` contains endpoint-specific workflow docs for usecase, flow, trouble, and troubleshooting, plus legacy deep dives.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Workflow index | `docs/README.md` | Starts at usecase -> flow -> trouble -> troubleshooting |
| Use cases | `docs/usecase/<module>/<controller>/<method>.md` | Actor-oriented requirements |
| Flows | `docs/flow/<module>/<controller>/<method>.md` | Implementation sequence and mechanics |
| Trouble | `docs/trouble/<module>/<controller>/<method>.md` | Before/after change rationale |
| Troubleshooting | `docs/troubleshooting/<module>/<controller>/<method>.md` | Fixes, metrics, and validation |
| Stage indexes | `docs/{usecase,flow,trouble,troubleshooting}/README.md` | Endpoint indexes by module/controller |
| Legacy deep dives | `docs/purchase/purchase-confirm-worker-version-mismatch.md`, `docs/usecase/waiting-queue.md`, `docs/flow/waiting-queue.md`, `docs/trouble/waiting-queue.md`, `docs/troubleshooting/waiting-queue.md` | Still useful narrative docs |
| Legacy module docs | `docs/{app,auth,broker,dispatcher,event,notification,purchase,seat,user}/README.md` | Kept for module-specific notes |
| Polling queue verification | `docs/polling-queue/` | Manual + command checklists |
| Gateway notes | `docs/platform/gateway/` | Routing, filters |
| Eureka notes | `docs/platform/eureka/` | Service discovery |
| Purchase deep dives | `docs/purchase/` | Legacy flow and planning docs |
| Common perf notes | `docs/platform/common/` | DB, batching, query analysis |

## Conventions
- Prefer concrete run commands and file paths over prose.
- Keep operational notes close to symptoms (troubleshooting) and link to root causes.
- When adding a new endpoint, create the four-doc chain:
  `usecase` -> `flow` -> `trouble` -> `troubleshooting`.

## Anti-Patterns
- Duplicating the same runbook in multiple module folders.
- Describing behavior without linking to the actual config or code location.
- Adding a troubleshooting note without showing the previous implementation or the observed fix.
- Adding a single giant file that collapses multiple endpoints.

## Suggested Doc Entry Points
| Goal | First Read |
|------|-----------|
| Understand routing | `docs/platform/gateway/README.md` |
| Understand queue | `docs/dispatcher/README.md` + `docs/broker/README.md` |
| Understand payment | `docs/purchase/README.md` + `docs/purchase/payment-flow.md` |
| Debug queue issues | `docs/troubleshooting/` |
| Start the new workflow | `docs/README.md` |


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
