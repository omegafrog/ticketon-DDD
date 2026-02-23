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