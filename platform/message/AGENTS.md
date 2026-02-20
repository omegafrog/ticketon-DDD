# platform/message

Cross-service message/event contracts (DTOs) used for messaging and integration.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Message types | `platform/message/src/main/java/org/codenbug/message/` | Keep backwards compatible |
| Kafka notes | `docs/platform/message/kafka.md` | Operational notes |

## Naming / Organization
| Guideline | Notes |
|----------|------|
| Event naming | Prefer explicit domain verbs/nouns |
| Package stability | Avoid moving classes across packages |
| Consumer impact | Check all downstream modules |

## Conventions
- Treat message types as published contracts.
- Prefer additive changes (new fields) over breaking removals/renames.
- When behavior changes, consider versioning or introducing a new message type.

## Anti-Patterns
- Renaming/removing fields without checking consumers.
- Using message DTOs as internal domain entities.

## Commands
```bash
./gradlew :platform:message:test
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