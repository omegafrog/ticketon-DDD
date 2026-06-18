#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TERRAFORM_DIR="$ROOT_DIR/infra/aws-ecs-free-tier"

IMAGE_TAG="${IMAGE_TAG:-${TF_VAR_image_tag:-latest}}"
AUTO_APPROVE="${AUTO_APPROVE:-true}"
WAIT_HTTP="${WAIT_HTTP:-true}"
SEED_SAMPLE_DATA="${SEED_SAMPLE_DATA:-false}"
SAMPLE_DATA_SQL="$TERRAFORM_DIR/sample-data.sql"
DEPLOY_FRONTEND="${DEPLOY_FRONTEND:-false}"

SERVICES=(eureka gateway app auth broker dispatcher)
declare -A GRADLE_TASKS=(
  [eureka]=":platform:eureka:build"
  [gateway]=":platform:gateway:build"
  [app]=":app:build"
  [auth]=":auth:build"
  [broker]=":broker:build"
  [dispatcher]=":dispatcher:build"
)
declare -A DOCKER_CONTEXTS=(
  [eureka]="platform/eureka"
  [gateway]="platform/gateway"
  [app]="app"
  [auth]="auth"
  [broker]="broker"
  [dispatcher]="dispatcher"
)

log() {
  printf '[deploy] %s\n' "$*"
}

die() {
  printf '[deploy] ERROR: %s\n' "$*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "missing command: $1"
}

require_env() {
  [[ -n "${!1:-}" ]] || die "missing env: $1"
}

resolve_vercel_origins() {
  local origins
  origins="$(python3 "$ROOT_DIR/.github/scripts/resolve-vercel-origins.py")"
  if [[ -n "$origins" ]]; then
    export TF_VAR_gateway_cors_allowed_origin_patterns="$origins"
    log "resolved Vercel frontend origins: $origins"
  else
    log "Vercel frontend origins not resolved; gateway CORS keeps configured default"
  fi
}

tf_apply_args() {
  if [[ "$AUTO_APPROVE" == "true" ]]; then
    printf '%s\n' "-auto-approve"
  fi
}

check_prereqs() {
  require_cmd aws
  require_cmd base64
  require_cmd docker
  require_cmd terraform
  require_cmd python3

  [[ -x "$ROOT_DIR/gradlew" ]] || die "missing executable gradlew"

  if [[ -z "${JAVA_HOME:-}" && -d "$HOME/.sdkman/candidates/java/current" ]]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi

  docker info >/dev/null 2>&1 || die "Docker daemon not reachable"
  aws sts get-caller-identity >/dev/null || die "AWS credentials not configured"
}

check_terraform_vars() {
  local tfvars="$TERRAFORM_DIR/terraform.tfvars"

  if [[ ! -f "$tfvars" ]]; then
    if [[ -n "${TF_VAR_db_host:-}" && -n "${TF_VAR_db_password:-}" && -n "${TF_VAR_rabbitmq_password:-}" ]]; then
      return 0
    fi

    cp "$TERRAFORM_DIR/terraform.tfvars.example" "$tfvars"
    die "created infra/aws-ecs-free-tier/terraform.tfvars. Replace db_host and passwords or export TF_VAR_db_host, TF_VAR_db_password, TF_VAR_rabbitmq_password, then rerun."
  fi

  local missing=()

  if grep -q 'replace-with-rds-endpoint' "$tfvars"; then
    missing+=("db_host")
  fi
  if grep -q 'replace-with-a-strong-password' "$tfvars"; then
    missing+=("db_password")
  fi
  if grep -q 'replace-with-a-strong-rabbitmq-password' "$tfvars"; then
    missing+=("rabbitmq_password")
  fi

  if (( ${#missing[@]} > 0 )); then
    die "placeholder secrets remain in terraform.tfvars: ${missing[*]}"
  fi
}

terraform_init() {
  log "terraform init"
  terraform -chdir="$TERRAFORM_DIR" init
}

ensure_ecr() {
  log "create/update ECR repositories"
  terraform -chdir="$TERRAFORM_DIR" apply \
    $(tf_apply_args) \
    -target=aws_ecr_repository.service \
    -target=aws_ecr_lifecycle_policy.service \
    -var="image_tag=$IMAGE_TAG"
}

read_ecr_repositories() {
  declare -gA ECR_REPOSITORIES=()
  local line service repo

  while IFS='=' read -r service repo; do
    ECR_REPOSITORIES["$service"]="$repo"
  done < <(
    terraform -chdir="$TERRAFORM_DIR" output -json ecr_repositories |
      python3 -c 'import json,sys; data=json.load(sys.stdin); [print(f"{k}={v}") for k,v in sorted(data.items())]'
  )

  for service in "${SERVICES[@]}"; do
    [[ -n "${ECR_REPOSITORIES[$service]:-}" ]] || die "missing ECR repository output for $service"
  done
}

docker_login() {
  local registries=()
  local service registry seen

  for service in "${SERVICES[@]}"; do
    registry="${ECR_REPOSITORIES[$service]%%/*}"
    seen=false
    for existing in "${registries[@]:-}"; do
      [[ "$existing" == "$registry" ]] && seen=true
    done
    if [[ "$seen" == "false" ]]; then
      registries+=("$registry")
    fi
  done

  for registry in "${registries[@]}"; do
    log "docker login $registry"
    aws ecr get-login-password --region "$(terraform -chdir="$TERRAFORM_DIR" output -raw aws_region)" |
      docker login --username AWS --password-stdin "$registry" >/dev/null
  done
}

build_jars() {
  local tasks=()
  local service

  for service in "${SERVICES[@]}"; do
    tasks+=("${GRADLE_TASKS[$service]}")
  done

  log "build runtime jars"
  (cd "$ROOT_DIR" && ./gradlew "${tasks[@]}" -x test --console=plain)
}

build_and_push_images() {
  local service context image dockerfile

  for service in "${SERVICES[@]}"; do
    context="${DOCKER_CONTEXTS[$service]}"
    dockerfile="$ROOT_DIR/$context/Dockerfile.runtime"
    image="${ECR_REPOSITORIES[$service]}:$IMAGE_TAG"

    [[ -f "$dockerfile" ]] || die "missing $context/Dockerfile.runtime"

    log "docker build/push $service -> $image"
    docker build -f "$dockerfile" -t "$image" "$ROOT_DIR/$context"
    docker push "$image"
  done
}

apply_ecs_service() {
  log "terraform apply ECS service"
  terraform -chdir="$TERRAFORM_DIR" apply \
    $(tf_apply_args) \
    -var="enable_ecs_service=true" \
    -var="image_tag=$IMAGE_TAG" \
    -var="gateway_cors_allowed_origin_patterns=${TF_VAR_gateway_cors_allowed_origin_patterns:-}"
}

force_deploy_and_wait() {
  CLUSTER_NAME="$(terraform -chdir="$TERRAFORM_DIR" output -raw cluster_name)"
  SERVICE_NAME="$(terraform -chdir="$TERRAFORM_DIR" output -raw service_name)"

  log "force ECS deployment: $CLUSTER_NAME/$SERVICE_NAME"
  aws ecs update-service \
    --cluster "$CLUSTER_NAME" \
    --service "$SERVICE_NAME" \
    --force-new-deployment >/dev/null

  log "wait ECS service stable"
  aws ecs wait services-stable \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME"
}

resolve_gateway_url() {
  local public_host public_ip

  log "resolve public gateway URL"
  INSTANCE_ID="$(
    aws ec2 describe-instances \
      --filters "Name=tag:Name,Values=${SERVICE_NAME}-ecs" "Name=instance-state-name,Values=running" \
      --query 'Reservations[].Instances[].InstanceId | [0]' \
      --output text
  )"
  [[ -n "$INSTANCE_ID" && "$INSTANCE_ID" != "None" ]] || die "running ECS instance ID not found"

  public_host="$(
    aws ec2 describe-instances \
      --filters "Name=tag:Name,Values=${SERVICE_NAME}-ecs" "Name=instance-state-name,Values=running" \
      --query 'Reservations[].Instances[].PublicDnsName | [0]' \
      --output text
  )"

  if [[ -z "$public_host" || "$public_host" == "None" ]]; then
    public_ip="$(
      aws ec2 describe-instances \
        --filters "Name=tag:Name,Values=${SERVICE_NAME}-ecs" "Name=instance-state-name,Values=running" \
        --query 'Reservations[].Instances[].PublicIpAddress | [0]' \
        --output text
    )"
    public_host="$public_ip"
  fi

  [[ -n "$public_host" && "$public_host" != "None" ]] || die "running ECS instance public host not found"
  GATEWAY_URL="http://$public_host:8080"
}

wait_gateway_http() {
  [[ "$WAIT_HTTP" == "true" ]] || return 0
  require_cmd curl

  log "wait gateway HTTP response"
  local code
  for _ in {1..60}; do
    code="$(curl -sS -o /dev/null -w '%{http_code}' --connect-timeout 3 --max-time 5 "$GATEWAY_URL" || true)"
    if [[ "$code" != "000" ]]; then
      log "gateway responded HTTP $code"
      return 0
    fi
    sleep 10
  done

  die "gateway did not answer within 10 minutes: $GATEWAY_URL"
}

dispatch_frontend_deploy() {
  if [[ "$DEPLOY_FRONTEND" != "true" ]]; then
    return 0
  fi

  require_cmd curl
  require_env FRONTEND_DISPATCH_TOKEN
  require_env FRONTEND_REPOSITORY

  local frontend_workflow="${FRONTEND_WORKFLOW:-deploy.yml}"
  local frontend_ref="${FRONTEND_REF:-main}"
  local payload

  payload="$(python3 - "$frontend_ref" "$GATEWAY_URL" <<'PY'
import json
import sys

ref, gateway_url = sys.argv[1:3]
print(json.dumps({
    "ref": ref,
    "inputs": {
        "backend_main_url": gateway_url,
        "backend_auth_url": gateway_url,
        "backend_queue_url": gateway_url,
    },
}))
PY
)"

  log "dispatch frontend deploy: $FRONTEND_REPOSITORY/$frontend_workflow -> $GATEWAY_URL"
  curl -fsS -X POST \
    -H "Accept: application/vnd.github+json" \
    -H "Authorization: Bearer $FRONTEND_DISPATCH_TOKEN" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "https://api.github.com/repos/$FRONTEND_REPOSITORY/actions/workflows/$frontend_workflow/dispatches" \
    -d "$payload" >/dev/null
}

seed_sample_data() {
  [[ "$SEED_SAMPLE_DATA" == "true" ]] || return 0
  die "SEED_SAMPLE_DATA=true is not supported in the RDS-backed ECS stack. Seed RDS directly with a MySQL client."
}

main() {
  check_prereqs
  check_terraform_vars
  resolve_vercel_origins
  terraform_init
  ensure_ecr
  read_ecr_repositories
  docker_login
  build_jars
  build_and_push_images
  apply_ecs_service
  force_deploy_and_wait
  resolve_gateway_url
  wait_gateway_http
  seed_sample_data
  dispatch_frontend_deploy

  printf '\nBackend deployed.\n'
  printf 'Gateway URL: %s\n' "$GATEWAY_URL"
  printf 'Frontend workflow dispatch: %s\n' "$([[ "$DEPLOY_FRONTEND" == "true" ]] && echo handled || echo skipped)"
}

main "$@"
