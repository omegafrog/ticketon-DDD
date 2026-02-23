# PROJECT KNOWLEDGE BASE

Generated: 2026-02-20
Branch: main
Commit: f746a34

This repo is a Gradle multi-module Java 21 / Spring Boot microservices system for ticket booking, organized with DDD-style module boundaries.

Note: `AGENTS.md` files are currently gitignored (see `.gitignore`). These instructions are for local/dev agent context unless you change ignore rules.

## Structure
```
./
├── app/                 # Orchestrator Spring Boot app (config imports only)
├── auth/                # Auth service (OAuth/JWT)
├── broker/              # SSE + waiting queue broker
├── dispatcher/          # Queue promotion/dispatch worker
├── event/               # Event read/write + queries
├── notification/        # Notification service
├── purchase/            # Payment/purchase service
├── seat/                # Seat layout/availability
├── user/                # User profile service
├── security-aop/        # Cross-cutting security annotations/aspects
├── redislock/           # Redis-based locking utilities
├── platform/            # Shared platform modules (common/message/gateway/eureka)
├── docker/              # Local infra (MySQL master/replica, Redis, RabbitMQ)
├── docs/                # Module docs + troubleshooting
├── k6/                  # Load test script(s)
└── .github/workflows/   # CI/CD workflows (build/push images, deploy)
```

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Add/modify API routes | `platform/gateway/src/main/resources/application*.yml` | Prod routes: `application-gateway-routes-prod.yml`; whitelists too |
| Orchestrate service wiring | `app/src/main/java/org/codenbug/app/config/` | App module should not contain business logic |
| Service entry points | `*/src/main/java/**/**Application.java` | Gateway/Eureka/App/Auth/Broker/Dispatcher have mains |
| Local infra setup | `docker/docker-compose.yml` | MySQL master+replica, Redis (3), RabbitMQ |
| Operational runbooks | `docs/troubleshooting/` | Queue/dispatch gotchas |
| Load testing | `k6/sse-throughput-test.js` | Talks to gateway/broker + Redis |

## Conventions (Repo-Specific)
- Java toolchain: 21 (`build.gradle`).
- Build system: Gradle multi-module (`settings.gradle`).
- Config is per-module: `*/src/main/resources/application*.yml` with `application-secret.yml` variants.
- Client traffic goes through gateway (port 8080); internal services register via Eureka.
- Some services/modules have committed build artifacts under `*/bin/`; treat `bin/` and `build/` as non-source.

## Anti-Patterns (This Repo)
- Do not put business logic, controllers, repositories, or infra wiring into `app/` beyond configuration imports.
- Do not put web (ServletRequest/Response, controllers) in domain/app packages meant to be pure application-layer logic.
- Do not commit or print secrets from `application-secret.yml` (these files are gitignored by default).

## Commands
```bash
# Build / test
./gradlew build
./gradlew test

# Run a specific service
./gradlew :platform:gateway:bootRun
./gradlew :platform:eureka:bootRun
./gradlew :app:bootRun
./gradlew :auth:bootRun
./gradlew :broker:bootRun
./gradlew :dispatcher:bootRun

# Local infra
docker-compose -f docker/docker-compose.yml up -d
```

## Runtime Verification Policy (Agent)
- When validating "server starts normally" or runtime execution results, prefer JDB MCP-based verification over log-only checks.
- Default flow:
  1) Start target service in debug mode (`--debug-jvm`) so JDWP opens on port `5005`.
  2) Attach via JDB MCP (or `jdb -attach 5005` as fallback) and verify thread/session visibility.
  3) Confirm service readiness via HTTP status/health endpoint.
  4) Stop the service unless persistence is explicitly requested.
- Keep verification evidence in temporary logs (for example under `/tmp`) and summarize key lines in the response.
- Do not expose secrets while collecting runtime evidence.

## Ports (Common)
- Gateway: 8080
- Eureka: 8761
- App: 9000
- Auth: 9001
- Broker: 9002
- Purchase: 9003
- User: 9004
- Seat: 9005
- MySQL: 3306 (master), 3307 (replica)
- Redis: 6379 (default)

## Credentials (Dev)
- User: `user1@ticketon.site` / `password123`
- Manager: `maanger@example.com` / `password123`

## Local DB Notes
- MySQL replica: `mysql -h 127.0.0.1 -P 3307 -uroot -ppassword`

## Nested Instructions
- Service/module specific guidance is in per-directory `AGENTS.md` files (e.g., `purchase/AGENTS.md`, `platform/gateway/AGENTS.md`).


## Pull Request Writing Policy (Agent)
- When creating a PR, write a detailed body by default.
- Include `## Summary`, `## Changes`, `## Verification`, and `## Risks and Rollback` sections.
- Explain scope boundaries (what is included and excluded) so reviewers can quickly assess impact.
- Link follow-up tasks or TODOs when work is intentionally deferred.