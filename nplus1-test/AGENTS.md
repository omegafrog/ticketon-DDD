# N+1 Test Module

Purpose: non-production query-performance checks and benchmark tests.

Look here:
- Bench mains: `nplus1-test/src/main/java/org/codenbug/bench/`
- Tests: `nplus1-test/src/test/java/org/codenbug/nplus1/`
- Test config: `nplus1-test/src/test/resources/application.yml`
- Seeds: `nplus1-test/src/test/resources/sql/`

Rules:
- Keep isolated from production runtime concerns.
- Use deterministic seeds and query assertions.
- Do not pull production secrets into this module.

Commands:
- `./gradlew :nplus1-test:test`
- `./gradlew :nplus1-test:test --tests '*NoNPlusOneTest'`
