# Security AOP (Cross-Cutting)

`security-aop/` provides security annotations and aspects used across services.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Aspects/annotations | `security-aop/src/main/java/org/codenbug/securityaop/aop/` | `@AuthNeeded`, `@RoleRequired`, etc. |
| User context | `security-aop/src/main/java/org/codenbug/securityaop/aop/LoggedInUserContext.java` | Propagates user info |
| Config/secrets | `security-aop/src/main/resources/application-secret.yml` | Sensitive |
| Docs | `docs/security-aop/` | AOP notes |

## Usage Pattern
| Goal | Hint | Notes |
|------|------|------|
| Require login | `@AuthNeeded` | Applied on controllers/handlers |
| Require role | `@RoleRequired(...)` | Centralized authz check |
| Access user | `LoggedInUserContext` | Prefer request-scoped context |

## Conventions
- Keep this module generic and reusable; avoid service-specific dependencies.
- Aspects should fail closed; avoid silent auth bypass.

## Anti-Patterns
- Parsing tokens in every service controller instead of using this module.
- Swallowing auth exceptions in aspects/handlers.

## Commands
```bash
./gradlew :security-aop:test
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