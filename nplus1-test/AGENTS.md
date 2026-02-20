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
