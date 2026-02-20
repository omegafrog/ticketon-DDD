# Documentation (docs/)

`docs/` contains module READMEs and troubleshooting/runbooks.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Cross-cutting index | `docs/platform/common/README.md` | Entry point to many deep dives |
| Module docs | `docs/{app,auth,broker,dispatcher,event,notification,purchase,seat,user}/README.md` | Per-module notes |
| Troubleshooting | `docs/troubleshooting/` | Queue/dispatch gotchas |
| Polling queue verification | `docs/polling-queue/` | Manual + command checklists |
| Gateway notes | `docs/platform/gateway/` | Routing, filters |
| Eureka notes | `docs/platform/eureka/` | Service discovery |
| Purchase deep dives | `docs/purchase/` | Payment flows and plans |
| Common perf notes | `docs/platform/common/` | DB, batching, query analysis |

## Conventions
- Prefer concrete run commands and file paths over prose.
- Keep operational notes close to symptoms (troubleshooting) and link to root causes.

## Anti-Patterns
- Duplicating the same runbook in multiple module folders.
- Describing behavior without linking to the actual config or code location.

## Suggested Doc Entry Points
| Goal | First Read |
|------|-----------|
| Understand routing | `docs/platform/gateway/README.md` |
| Understand queue | `docs/dispatcher/README.md` + `docs/broker/README.md` |
| Understand payment | `docs/purchase/README.md` + `docs/purchase/payment-flow.md` |
| Debug queue issues | `docs/troubleshooting/` |
