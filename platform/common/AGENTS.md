# platform/common

Shared utilities used across services (exceptions, shared config helpers, common DTOs).

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Shared utilities | `platform/common/src/main/java/` | Keep small and stable |
| Secrets | `platform/common/src/main/resources/application-secret.yml` | Sensitive; often shared JWT/etc |

## Typical Contents
| Area | Notes |
|------|------|
| Exceptions | Shared exception types used across services |
| Redis helpers | Common Redis client/config patterns |
| DTO helpers | Lightweight shared DTO/value objects |

## Conventions
- Avoid service-specific logic; this module should not depend on business modules.
- Prefer additive changes; breaking API changes ripple across services.
- Keep `application-secret.yml` out of logs and commits.

## Anti-Patterns
- Adding business rules here because it is convenient.
- Adding dependencies from `platform/common` to a specific service module.

## Commands
```bash
./gradlew :platform:common:test
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