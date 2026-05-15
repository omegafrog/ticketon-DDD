# Docker Local Infra

Purpose: local RabbitMQ, Redis, MySQL master/replica, and related setup.

Look here:
- Compose: `docker/docker-compose.yml`
- MySQL config: `docker/mysql/`
- Replication setup: `docker/setup-replication.sh`
- Local data dirs: `docker/redis/`, `docker/cache/`, `docker/polling/`

Rules:
- Treat certs, keys, DB dumps, and local data as sensitive.
- Prefer resetting containers over editing generated DB volumes.
- MySQL replica depends on master healthcheck; startup can take time.

Commands:
- `docker-compose -f docker/docker-compose.yml ps`
- `docker-compose -f docker/docker-compose.yml up -d`
- Destructive reset only on explicit request: `docker-compose -f docker/docker-compose.yml down -v`
