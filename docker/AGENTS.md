# Local Infra (docker/)

`docker/` contains local infrastructure configuration and scripts.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Compose | `docker/docker-compose.yml` | RabbitMQ, Redis (3), MySQL master/replica |
| MySQL config | `docker/mysql/` | master/replica cnf |
| Replication scripts | `docker/setup-replication.sh` | Local DB replication setup |
| Data dirs | `docker/redis/`, `docker/cache/`, `docker/polling/` | Local persistence |
| Backup | `docker/backup.sql` | Large; treat as data artifact |

## Compose Services (Local)
| Service | Ports | Notes |
|---------|-------|------|
| RabbitMQ | 5672, 15672 | Management UI on 15672 |
| Redis | 6379 | Default |
| Redis (cache) | 6380 | Separate instance |
| Redis (polling) | 6381 | Separate instance |
| MySQL master | 3306 | Healthcheck gated |
| MySQL replica | 3307 | Depends on master |

## Conventions
- Treat `docker/*.pem` and other cert/key materials as sensitive.
- Avoid editing generated DB volumes; prefer resetting containers.

## Gotchas
- MySQL replica depends on master healthchecks; startup can take time.
- Redis uses 3 instances (default/cache/polling); make sure ports don't conflict.

## Commands
```bash
docker-compose -f docker/docker-compose.yml up -d
docker-compose -f docker/docker-compose.yml ps
docker-compose -f docker/docker-compose.yml logs -f

# Full reset (destructive)
docker-compose -f docker/docker-compose.yml down -v
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
