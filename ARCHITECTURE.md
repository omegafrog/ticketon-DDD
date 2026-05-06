# Ticketon DDD Architecture

## 1. Purpose

This file is the executor-facing architecture constraint for implementation work.
Executors must read this file before changing production or test code.

The repository is an existing Java 21 / Spring Boot Gradle multi-module system.
Implementation work should modify the current modules toward the design documents
under `docs/design` instead of replacing the project with a new baseline.

## 2. Root Package And Modules

- Root package: `org.codenbug`
- Build tool: Gradle
- Java version: 21
- Architecture style: DDD-oriented layered modules

Existing modules:

| Gradle module | Responsibility |
|---|---|
| `app` | Orchestrator Spring Boot app and cross-service configuration imports only |
| `auth` | Auth/Authz BC: signup, login, social account linking, roles, access tokens |
| `user` | User/Profile BC: user profile and personal information processing |
| `event` | Event BC: event operation, public discovery, event image handling |
| `broker` | Queue BC API/runtime entry point for polling waiting queue concerns |
| `dispatcher` | Queue promotion/dispatch worker for slot promotion and cleanup |
| `seat` | Seat BC: seat layout, occupancy, release, sale confirmation |
| `purchase` | Booking/Payment BC: reservation, purchase/payment, cancellation, refund |
| `notification` | Existing notification module. Current design excludes notification implementation unless a later plan reintroduces it |
| `security-aop` | Cross-cutting security annotations/aspects only |
| `redislock` | Redis lock utility infrastructure only |
| `platform:common` | Shared platform/common types only |
| `platform:message` | Published messages and integration contracts |
| `platform:gateway` | Gateway routes and edge routing configuration |
| `platform:eureka` | Service discovery runtime |
| `nplus1-test` | Isolated performance/test support module |

## 3. Bounded Context Mapping

The implementation must follow the current design BC boundaries:

| Bounded context | Primary modules |
|---|---|
| Auth/Authz BC | `auth`, with cross-cutting enforcement in `security-aop` |
| User/Profile BC | `user` |
| Event BC | `event` |
| Queue BC | `broker`, `dispatcher` |
| Seat BC | `seat` |
| Booking/Payment BC | `purchase` |

BC decisions from design:

- User information processing and authentication/authorization are separate BCs.
- Event operation, public discovery, and event image handling belong to one Event BC.
- Reservation, purchase/payment, cancellation, and refund belong to one Booking/Payment BC.
- Seat remains separate because duplicate-sale prevention and high-contention inventory consistency have a distinct model.

## 4. Layering Rules

Feature modules should converge toward these package responsibilities. Existing
package names may differ; implementation should improve boundaries incrementally.

| Layer/package intent | Responsibility | Forbidden |
|---|---|---|
| `presentation` or existing `ui` | HTTP controllers, request/response DTO mapping | Business rules, direct persistence calls |
| `application` or existing `app` | Use case orchestration, transaction boundary, ports | Domain rule ownership, direct infrastructure implementation coupling |
| `domain` | Entities, value objects, aggregates, domain events, domain exceptions | Spring MVC, JPA repositories, Redis, HTTP clients |
| `infrastructure` or existing `infra` | Persistence, messaging, Redis, HTTP clients, storage adapters | Controller logic, domain policy ownership |
| `api` | Stable contracts exposed to other modules | Internal domain object leakage |

Domain behavior must be expressed through aggregate root methods and value object
validation. Setters and direct child-object mutation are not allowed for new or
rewritten aggregate behavior.

## 5. Dependency Direction

- `app` may wire modules but must not contain business logic, controllers, repositories, or infrastructure adapters beyond configuration imports already required by the existing runtime.
- Feature module `presentation/ui` depends on application contracts.
- Application layer depends on domain and outbound ports, not infrastructure implementations.
- Domain layer does not depend on Spring web, persistence, Redis, RabbitMQ, HTTP clients, or other modules' internals.
- Infrastructure implements application outbound ports.
- Cross-BC access must use one of:
  - another BC's explicit API contract,
  - projection maintained by messaging,
  - internal API only when strong consistency/concurrency requires it.
- Feature modules must not depend on another feature module's `domain`, `infra`, `infrastructure`, `ui`, or `presentation` packages.
- Shared module usage must stay limited to genuinely common technical contracts and platform types.

## 6. Messaging And Integration

- Use RabbitMQ-backed projections for cross-BC read models where design requires async lookup.
- Use internal APIs only for cross-BC interactions that need synchronous consistency or concurrency control.
- Use the outbox pattern for message publishing.
- Message handling must tolerate duplicates and preserve ordering per aggregate key.
- After 5 retries, message handling must transition to failure handling.
- Toss is the only payment provider target for implementation; if infrastructure details are unclear, keep a port/interface and defer adapter implementation.
- Static image storage must support local and external implementations; dev profile uses local storage.

## 7. Security, Audit, And Privacy

- Passwords must be encrypted.
- Manager authority is limited to events created by that manager.
- Manager refund authority is limited to that manager's events.
- Authorized API calls must be audit logged with request body, response body, user information, time, and any extra fields needed for traceability.
- Audit log retention target is 1 month.
- Do not print or commit secrets from `application-secret.yml` or environment-specific secret files.

## 8. Test And Verification Expectations

Executors must update tests near the implementation they change.

- Domain and aggregate tests cover state transitions, invariants, value object validation, and rule violations.
- Application service tests cover orchestration, ports, repository interactions, external collaboration ports, and compensation paths.
- Infrastructure tests cover persistence mappings, Redis, RabbitMQ messaging, local/external image storage adapters, serialization, and HTTP clients where applicable.
- Contract tests should be written as unit tests.
- Integration tests should cover happy paths only unless the plan explicitly calls out more.
- Use Testcontainers for integration tests that need real infrastructure, except fully external providers such as Toss.

Verification commands normally include:

```bash
./gradlew build
./gradlew test
```

If architecture linting is present or added by the plan, also run the configured
architecture/static-analysis commands such as `./gradlew architectureRules` and
Semgrep with the repository's DDD rules.

## 9. Runtime Verification

When validating that a service starts normally:

1. Start the target service in debug mode with `--debug-jvm`.
2. Attach via JDB MCP or `jdb -attach 5005`.
3. Confirm service readiness through HTTP status or health endpoint.
4. Stop the service unless persistence is explicitly requested.

Keep runtime evidence in temporary logs and do not expose secrets.
