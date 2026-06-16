#!/usr/bin/env bash
set -euo pipefail

source /etc/ticketon-deploy.env

aws ecs describe-services \
  --region "$AWS_REGION" \
  --cluster "$ECS_CLUSTER_NAME" \
  --services "$ECS_SERVICE_NAME" \
  --query 'services[0].deployments[?rolloutState!=`COMPLETED`]' \
  --output text | grep -q '^$'

curl -fsS --retry 12 --retry-delay 5 http://127.0.0.1:8080/actuator/health >/dev/null
