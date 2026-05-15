# Repo Context for Agents

This file holds context that used to live in root `AGENTS.md`. Read this only when repository orientation is needed.

## Architecture

Gradle multi-module Java 21 / Spring Boot ticket-booking system.

Modules:
- `app`: orchestration app, config imports and wiring only.
- `auth`: OAuth/JWT auth service.
- `broker`: SSE and Redis waiting queue broker.
- `dispatcher`: queue promotion worker.
- `event`: event write/read/query service.
- `notification`: notification service.
- `purchase`: payment, purchase, refund service.
- `seat`: seat layout and availability service.
- `user`: user profile service.
- `security-aop`: shared auth annotations/aspects.
- `redislock`: Redis-based locking utilities.
- `platform/common`: shared utilities.
- `platform/message`: message/event contracts.
- `platform/gateway`: Spring Cloud Gateway.
- `platform/eureka`: service discovery.
- `docker`: local MySQL, Redis, RabbitMQ.
- `k6`: load tests.

## Common Lookup Table

| Task | Location |
| --- | --- |
| Gateway routes | `platform/gateway/src/main/resources/application*.yml` |
| Gateway whitelist | `platform/gateway/src/main/resources/application-whitelist-*.yml` |
| App wiring | `app/src/main/java/org/codenbug/app/config/` |
| Service mains | `*/src/main/java/**/**Application.java` |
| Local infra | `docker/docker-compose.yml` |
| Endpoint docs | `docs/{usecase,flow,trouble,troubleshooting}/<module>/<controller>/<method>.md` |
| Load test | `k6/sse-throughput-test.js` |

## Ports

| Service | Port |
| --- | --- |
| Gateway | 8080 |
| Eureka | 8761 |
| App | 9000 |
| Auth | 9001 |
| Broker | 9002 |
| Purchase | 9003 |
| User | 9004 |
| Seat | 9005 |
| MySQL master | 3306 |
| MySQL replica | 3307 |
| Redis default | 6379 |

## Repo-Specific Traps

- `app/` must not contain service business logic.
- `platform/message` changes are contract changes; check consumers.
- `dispatcher` and `broker` changes are queue/concurrency sensitive.
- `purchase` state transitions need explicit locking/version reasoning.
- Some `bin/` and `build/` files exist; treat as generated unless under `src/`.
- Secret variants are ignored; never print values from `application-secret.yml`.
