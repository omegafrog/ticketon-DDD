# N+1 Test / Bench Module

`nplus1-test/` is a non-production module used for query-performance checks and benchmarks.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Bench mains | `nplus1-test/src/main/java/org/codenbug/bench/` | Local benchmark runners |
| N+1 tests | `nplus1-test/src/test/java/org/codenbug/nplus1/` | `*NoNPlusOneTest` etc. |
| Test config | `nplus1-test/src/test/resources/application.yml` | Uses in-memory DB config |
| Seeds | `nplus1-test/src/test/resources/sql/` | Deterministic data |
| Docs | `docs/nplus1/` | Notes per domain area |

## What This Module Validates
| Check | How |
|-------|-----|
| Query shape | Repository/query tests (e.g., left join vs N+1) |
| Seed compatibility | SQL seeds must match entity mappings |
| Regression guard | Keep tests deterministic and fast |

## Conventions
- Keep this module isolated from production runtime concerns.
- Prefer deterministic DB seeds for stable query assertions.

## Anti-Patterns
- Making assertions depend on timing or non-deterministic data.
- Pulling production secrets into this module.

## Commands
```bash
./gradlew :nplus1-test:test

# Run a single test
./gradlew :nplus1-test:test --tests '*NoNPlusOneTest'
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