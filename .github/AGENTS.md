# GitHub Workflows (.github/)

Workflows build and push service images to GHCR and deploy via AWS SSM.

## Where To Look
| Task | Location | Notes |
|------|----------|------|
| Per-service CI | `.github/workflows/*-ci.yml` | gateway/eureka/app/auth/broker/dispatcher |
| Full release CI | `.github/workflows/common-ci.yml` | workflow_dispatch; runs tests + builds images |
| Deploy | `.github/workflows/deploy.yml` | workflow_dispatch; pulls config from AWS SSM |

## Workflow Inventory
| Workflow | Purpose |
|----------|---------|
| `.github/workflows/gateway-ci.yml` | Build/push gateway image |
| `.github/workflows/eureka-ci.yml` | Build/push eureka image |
| `.github/workflows/app-ci.yml` | Build/push app image |
| `.github/workflows/auth-ci.yml` | Build/push auth image |
| `.github/workflows/broker-ci.yml` | Build/push broker image |
| `.github/workflows/disptach-ci.yml` | Build/push dispatcher image (misspelled) |
| `.github/workflows/common-ci.yml` | Multi-service test + build + tag |
| `.github/workflows/deploy.yml` | Manual deploy via SSM |

## CI Conventions
| Topic | Notes |
|------|------|
| Image build | Uses Docker Buildx + `docker/build-push-action` |
| Registry | GHCR images tagged with `:latest` and release tag |
| Service selection | Most workflows trigger on path filters per module |
| Dispatcher workflow | File is `disptach-ci.yml` (misspelled) |

## Deploy Conventions
- Deployment uses AWS SSM to fetch `application*.yml` into `/config` then runs a container.
- Do not paste AWS credentials or SSM parameter values into logs.

## Common Gotchas
- Workflow names and docker image names must stay consistent with deploy.yml.
- Dispatcher workflow filename is misspelled; be careful when referencing it.

## Conventions
- CI primarily builds Docker images using service Dockerfiles.
- Deployment expects AWS secrets and uses SSM to run docker commands remotely.


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