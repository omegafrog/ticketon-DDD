#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage:
  ./stack-power.sh pause
  ./stack-power.sh resume

Pauses or resumes only AWS compute/database resources currently tracked in this
Terraform state. Non-stoppable resources such as ECR, S3, VPC, EBS snapshots,
Elastic IPs, NAT gateways, and load balancers are not changed.
USAGE
}

die() {
  echo "error: $*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "$1 is required"
}

action="${1:-}"
case "$action" in
  pause|resume) ;;
  *) usage; exit 2 ;;
esac

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir"

require_cmd aws
require_cmd python3
require_cmd terraform

state_file="${STACK_POWER_STATE_FILE:-$script_dir/.terraform/stack-power-state.json}"
inventory_file="$(mktemp)"
trap 'rm -f "$inventory_file"' EXIT

terraform show -json > "$inventory_file"

region="$(
  python3 - "$inventory_file" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    data = json.load(f)

outputs = data.get("values", {}).get("outputs", {})
if outputs.get("aws_region", {}).get("value"):
    print(outputs["aws_region"]["value"])
    raise SystemExit

def modules(module):
    yield module
    for child in module.get("child_modules", []) or []:
        yield from modules(child)

root = data.get("values", {}).get("root_module", {}) or {}
for module in modules(root):
    for res in module.get("resources", []) or []:
        if res.get("type") == "aws_region":
            name = (res.get("values") or {}).get("name") or (res.get("values") or {}).get("id")
            if name:
                print(name)
                raise SystemExit
PY
)"

region="${AWS_REGION:-${AWS_DEFAULT_REGION:-${region:-ap-northeast-2}}}"

if [[ "$action" == "pause" ]]; then
  mkdir -p "$(dirname "$state_file")"
  python3 - "$inventory_file" > "$state_file" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    data = json.load(f)

out = {
    "autoscaling_groups": [],
    "ecs_services": [],
    "ec2_instances": [],
    "rds_instances": [],
    "rds_clusters": [],
}

def modules(module):
    yield module
    for child in module.get("child_modules", []) or []:
        yield from modules(child)

root = data.get("values", {}).get("root_module", {}) or {}
for module in modules(root):
    for res in module.get("resources", []) or []:
        if res.get("mode") != "managed":
            continue
        values = res.get("values") or {}
        typ = res.get("type")
        address = res.get("address")

        if typ == "aws_autoscaling_group" and values.get("name"):
            out["autoscaling_groups"].append({
                "address": address,
                "name": values["name"],
                "min_size": values.get("min_size", 0),
                "max_size": values.get("max_size", 0),
                "desired_capacity": values.get("desired_capacity", 0),
            })
        elif typ == "aws_ecs_service" and values.get("name") and values.get("cluster"):
            out["ecs_services"].append({
                "address": address,
                "cluster": values["cluster"],
                "name": values["name"],
                "desired_count": values.get("desired_count", 0),
            })
        elif typ == "aws_instance" and values.get("id"):
            out["ec2_instances"].append({
                "address": address,
                "id": values["id"],
            })
        elif typ == "aws_db_instance":
            identifier = values.get("identifier") or values.get("id")
            if identifier:
                out["rds_instances"].append({
                    "address": address,
                    "identifier": identifier,
                })
        elif typ == "aws_rds_cluster":
            identifier = values.get("cluster_identifier") or values.get("id")
            if identifier:
                out["rds_clusters"].append({
                    "address": address,
                    "identifier": identifier,
                })

json.dump(out, sys.stdout, indent=2, sort_keys=True)
print()
PY

  echo "pause state saved: $state_file"

  python3 - "$state_file" <<'PY' | while IFS=$'\t' read -r kind a b c d; do
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    state = json.load(f)

for svc in state["ecs_services"]:
    print("ecs", svc["cluster"], svc["name"], "0", "", sep="\t")
for asg in state["autoscaling_groups"]:
    print("asg", asg["name"], "0", "0", "", sep="\t")
for ec2 in state["ec2_instances"]:
    print("ec2", ec2["id"], "", "", "", sep="\t")
for db in state["rds_instances"]:
    print("rds-instance", db["identifier"], "", "", "", sep="\t")
for cluster in state["rds_clusters"]:
    print("rds-cluster", cluster["identifier"], "", "", "", sep="\t")
PY
    case "$kind" in
      ecs)
        echo "pausing ECS service: $b"
        aws ecs update-service --region "$region" --cluster "$a" --service "$b" --desired-count "$c" >/dev/null
        ;;
      asg)
        echo "pausing Auto Scaling group: $a"
        aws autoscaling update-auto-scaling-group --region "$region" --auto-scaling-group-name "$a" --min-size "$b" --desired-capacity "$c"
        ;;
      ec2)
        echo "stopping EC2 instance: $a"
        aws ec2 stop-instances --region "$region" --instance-ids "$a" >/dev/null
        ;;
      rds-instance)
        echo "stopping RDS instance: $a"
        aws rds stop-db-instance --region "$region" --db-instance-identifier "$a" >/dev/null
        ;;
      rds-cluster)
        echo "stopping RDS cluster: $a"
        aws rds stop-db-cluster --region "$region" --db-cluster-identifier "$a" >/dev/null
        ;;
    esac
  done

  echo "pause requested. RDS stopped state lasts up to 7 days, then AWS may restart it."
  exit 0
fi

[[ -f "$state_file" ]] || die "missing pause state: $state_file"

python3 - "$state_file" <<'PY' | while IFS=$'\t' read -r kind a b c d; do
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    state = json.load(f)

for asg in state["autoscaling_groups"]:
    print("asg", asg["name"], str(asg["min_size"]), str(asg["max_size"]), str(asg["desired_capacity"]), sep="\t")
for ec2 in state["ec2_instances"]:
    print("ec2", ec2["id"], "", "", "", sep="\t")
for db in state["rds_instances"]:
    print("rds-instance", db["identifier"], "", "", "", sep="\t")
for cluster in state["rds_clusters"]:
    print("rds-cluster", cluster["identifier"], "", "", "", sep="\t")
for svc in state["ecs_services"]:
    print("ecs", svc["cluster"], svc["name"], str(svc["desired_count"]), "", sep="\t")
PY
  case "$kind" in
    asg)
      echo "resuming Auto Scaling group: $a"
      aws autoscaling update-auto-scaling-group --region "$region" --auto-scaling-group-name "$a" --max-size "$c" --min-size "$b" --desired-capacity "$d"
      ;;
    ec2)
      echo "starting EC2 instance: $a"
      aws ec2 start-instances --region "$region" --instance-ids "$a" >/dev/null
      ;;
    rds-instance)
      echo "starting RDS instance: $a"
      aws rds start-db-instance --region "$region" --db-instance-identifier "$a" >/dev/null
      aws rds wait db-instance-available --region "$region" --db-instance-identifier "$a"
      ;;
    rds-cluster)
      echo "starting RDS cluster: $a"
      aws rds start-db-cluster --region "$region" --db-cluster-identifier "$a" >/dev/null
      aws rds wait db-cluster-available --region "$region" --db-cluster-identifier "$a"
      ;;
    ecs)
      echo "resuming ECS service: $b"
      aws ecs update-service --region "$region" --cluster "$a" --service "$b" --desired-count "$c" >/dev/null
      ;;
  esac
done

echo "resume requested."
