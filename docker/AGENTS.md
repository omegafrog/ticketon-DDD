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
