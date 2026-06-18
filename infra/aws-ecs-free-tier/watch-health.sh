#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage:
  ./watch-health.sh [interval-seconds]

Repeatedly prints AWS/ECS/RDS/Gateway health for this free-tier stack.
Stop with Ctrl+C.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

interval="${1:-5}"
case "$interval" in
  ''|*[!0-9]*) echo "error: interval must be seconds" >&2; exit 2 ;;
esac

command -v aws >/dev/null 2>&1 || { echo "error: aws is required" >&2; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "error: curl is required" >&2; exit 1; }

export AWS_PAGER=""

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir"

tf_output() {
  terraform output -raw "$1" 2>/dev/null || true
}

region="${AWS_REGION:-${AWS_DEFAULT_REGION:-$(tf_output aws_region)}}"
region="${region:-ap-northeast-2}"
service_name="$(tf_output service_name)"
service_name="${service_name:-ticketon-dev}"
cluster_name="$(tf_output cluster_name)"
cluster_name="${cluster_name:-$service_name}"
asg_name="${service_name}-ecs"
rds_id="${RDS_ID:-${service_name}-mysql}"

line() {
  printf '%-16s %s\n' "$1" "$2"
}

while true; do
  clear || true
  echo "Ticketon AWS health $(date '+%Y-%m-%d %H:%M:%S')"
  echo "region=$region cluster=$cluster_name service=$service_name"
  echo

  ecs_json="$(aws ecs describe-services --region "$region" --cluster "$cluster_name" --services "$service_name" --output json 2>/dev/null || true)"
  if [[ -n "$ecs_json" ]]; then
    python3 - "$ecs_json" <<'PY'
import json
import sys

data = json.loads(sys.argv[1])
failures = data.get("failures") or []
services = data.get("services") or []
if failures:
    print(f"ECS service      ERROR {failures[0].get('reason', 'unknown')}")
elif not services:
    print("ECS service      MISSING")
else:
    s = services[0]
    ok = s.get("desiredCount") == s.get("runningCount") and s.get("pendingCount") == 0
    state = "OK" if ok else "WAIT"
    print(f"ECS service      {state} desired={s.get('desiredCount')} running={s.get('runningCount')} pending={s.get('pendingCount')} status={s.get('status')}")
PY
  else
    line "ECS service" "ERROR describe failed"
  fi

  task_arns="$(aws ecs list-tasks --region "$region" --cluster "$cluster_name" --service-name "$service_name" --query 'taskArns' --output text 2>/dev/null || true)"
  if [[ -n "$task_arns" && "$task_arns" != "None" ]]; then
    task_json="$(aws ecs describe-tasks --region "$region" --cluster "$cluster_name" --tasks $task_arns --output json 2>/dev/null || true)"
    python3 - "$task_json" <<'PY'
import json
import sys

data = json.loads(sys.argv[1])
tasks = data.get("tasks") or []
if not tasks:
    print("ECS tasks        MISSING")
else:
    for task in tasks:
        last = task.get("lastStatus")
        desired = task.get("desiredStatus")
        health = task.get("healthStatus", "UNKNOWN")
        short = task.get("taskArn", "").rsplit("/", 1)[-1][:12]
        print(f"ECS task         {short} last={last} desired={desired} health={health}")
        for c in task.get("containers") or []:
            name = c.get("name")
            status = c.get("lastStatus")
            chealth = c.get("healthStatus", "UNKNOWN")
            exit_code = c.get("exitCode")
            extra = f" exit={exit_code}" if exit_code is not None else ""
            ok = "OK" if status == "RUNNING" and chealth in ("HEALTHY", "UNKNOWN") else "WAIT"
            print(f"  container      {ok} {name} status={status} health={chealth}{extra}")
PY
  else
    line "ECS tasks" "none"
  fi

  asg_json="$(aws autoscaling describe-auto-scaling-groups --region "$region" --auto-scaling-group-names "$asg_name" --output json 2>/dev/null || true)"
  if [[ -n "$asg_json" ]]; then
    python3 - "$asg_json" <<'PY'
import json
import sys

data = json.loads(sys.argv[1])
groups = data.get("AutoScalingGroups") or []
if not groups:
    print("ASG              MISSING")
else:
    g = groups[0]
    instances = g.get("Instances") or []
    print(f"ASG              desired={g.get('DesiredCapacity')} min={g.get('MinSize')} max={g.get('MaxSize')} instances={len(instances)}")
    for i in instances:
        print(f"  ASG instance   {i.get('InstanceId')} lifecycle={i.get('LifecycleState')} health={i.get('HealthStatus')}")
PY
  else
    line "ASG" "ERROR describe failed"
  fi

  ec2_json="$(aws ec2 describe-instances --region "$region" --filters "Name=tag:Name,Values=${service_name}-ecs" "Name=instance-state-name,Values=pending,running,stopping,stopped" --output json 2>/dev/null || true)"
  public_host=""
  if [[ -n "$ec2_json" ]]; then
    ec2_summary="$(python3 - "$ec2_json" <<'PY'
import json
import sys

data = json.loads(sys.argv[1])
instances = [i for r in data.get("Reservations", []) for i in r.get("Instances", [])]
if not instances:
    raise SystemExit
for i in instances:
    iid = i.get("InstanceId")
    state = (i.get("State") or {}).get("Name")
    ip = i.get("PublicIpAddress") or ""
    dns = i.get("PublicDnsName") or ""
    print(f"{dns or ip}\tEC2              {iid} state={state} public={dns or ip}")
PY
)"
    public_host="${ec2_summary%%$'\t'*}"
    ec2_line="${ec2_summary#*$'\t'}"
    [[ -n "$ec2_line" && "$ec2_line" != "$ec2_summary" ]] && echo "$ec2_line"
  fi

  if [[ -n "$public_host" ]]; then
    line "Gateway URL" "http://$public_host:8080"
    code="$(curl -sS -o /dev/null -w '%{http_code}' --connect-timeout 2 --max-time 4 "http://$public_host:8080/actuator/health" 2>/dev/null || true)"
    [[ "$code" == "000" || -z "$code" ]] && code="$(curl -sS -o /dev/null -w '%{http_code}' --connect-timeout 2 --max-time 4 "http://$public_host:8080" 2>/dev/null || true)"
    if [[ "$code" != "000" && -n "$code" ]]; then
      line "Gateway HTTP" "OK http=$code"
    else
      line "Gateway HTTP" "WAIT no response"
    fi
  else
    line "EC2" "none running/stopped"
    line "Gateway HTTP" "WAIT no public host"
  fi

  rds_status="$(aws rds describe-db-instances --region "$region" --db-instance-identifier "$rds_id" --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || true)"
  if [[ -n "$rds_status" && "$rds_status" != "None" ]]; then
    if [[ "$rds_status" == "available" ]]; then
      line "RDS" "OK $rds_id status=$rds_status"
    else
      line "RDS" "WAIT $rds_id status=$rds_status"
    fi
  else
    line "RDS" "MISSING $rds_id"
  fi

  echo
  echo "refresh=${interval}s  stop=Ctrl+C"
  sleep "$interval"
done
