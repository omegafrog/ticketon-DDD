#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage:
  ./stack-power.sh pause
  ./stack-power.sh stop
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
  pause|stop|resume) ;;
  *) usage; exit 2 ;;
esac

[[ "$action" == "stop" ]] && action="pause"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir"

require_cmd aws
require_cmd python3
require_cmd terraform

state_file="${STACK_POWER_STATE_FILE:-$script_dir/.terraform/stack-power-state.json}"
work_dir="$(mktemp -d)"
inventory_file="$work_dir/inventory.json"
asg_file="$work_dir/asg.json"
ecs_file="$work_dir/ecs.json"
trap 'rm -rf "$work_dir"' EXIT

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
stack_name="$(terraform console <<< 'local.name' | tr -d '"')"

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
                "instance_ids": [],
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

  # A targeted or interrupted apply can leave live stack resources absent from
  # local state. Discover this stack's conventionally named compute resources
  # so pause cannot silently succeed while they remain online.
  aws autoscaling describe-auto-scaling-groups \
    --region "$region" \
    --auto-scaling-group-names "${stack_name}-ecs" \
    --output json > "$asg_file"
  aws ecs describe-services \
    --region "$region" \
    --cluster "$stack_name" \
    --services "$stack_name" \
    --output json > "$ecs_file"

  python3 - "$state_file" "$asg_file" "$ecs_file" "$stack_name" <<'PY'
import json
import sys

state_path, asg_path, ecs_path, stack_name = sys.argv[1:]
with open(state_path, encoding="utf-8") as f:
    state = json.load(f)
with open(asg_path, encoding="utf-8") as f:
    asg_data = json.load(f)
with open(ecs_path, encoding="utf-8") as f:
    ecs_data = json.load(f)

known_asgs = {item["name"]: item for item in state["autoscaling_groups"]}
for group in asg_data.get("AutoScalingGroups", []):
    name = group.get("AutoScalingGroupName")
    if not name:
        continue
    instance_ids = [
        instance["InstanceId"]
        for instance in group.get("Instances", [])
        if instance.get("InstanceId")
    ]
    if name in known_asgs:
        known_asgs[name]["instance_ids"] = instance_ids
    else:
        item = {
            "address": "discovered-from-aws",
            "name": name,
            "min_size": group.get("MinSize", 0),
            "max_size": group.get("MaxSize", 0),
            "desired_capacity": group.get("DesiredCapacity", 0),
            "instance_ids": instance_ids,
        }
        state["autoscaling_groups"].append(item)
        known_asgs[name] = item

known_services = {(item["cluster"], item["name"]) for item in state["ecs_services"]}
for service in ecs_data.get("services", []):
    name = service.get("serviceName")
    if name and (stack_name, name) not in known_services:
        state["ecs_services"].append({
            "address": "discovered-from-aws",
            "cluster": stack_name,
            "name": name,
            "desired_count": service.get("desiredCount", 0),
        })

if not any(state[key] for key in state):
    raise SystemExit("error: no stoppable resources found in Terraform state or AWS")

with open(state_path, "w", encoding="utf-8") as f:
    json.dump(state, f, indent=2, sort_keys=True)
    f.write("\n")
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
    print("asg", asg["name"], "", "", "", sep="\t")
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
        echo "suspending Auto Scaling group processes: $a"
        aws autoscaling suspend-processes --region "$region" --auto-scaling-group-name "$a"
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

  python3 - "$state_file" <<'PY' | while IFS=$'\t' read -r cluster service; do
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    state = json.load(f)
for service in state["ecs_services"]:
    print(service["cluster"], service["name"], sep="\t")
PY
    echo "waiting for ECS service to stop: $service"
    aws ecs wait services-stable --region "$region" --cluster "$cluster" --services "$service"
  done

  python3 - "$state_file" <<'PY' | while IFS=$'\t' read -r asg instance_ids; do
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    state = json.load(f)
for group in state["autoscaling_groups"]:
    print(group["name"], " ".join(group.get("instance_ids", [])), sep="\t")
PY
    [[ -n "$instance_ids" ]] || continue
    read -r -a ids <<< "$instance_ids"
    echo "stopping Auto Scaling group EC2 instances: $asg"
    aws ec2 stop-instances --region "$region" --instance-ids "${ids[@]}" >/dev/null
    aws ec2 wait instance-stopped --region "$region" --instance-ids "${ids[@]}"
  done

  echo "pause completed. RDS stopped state lasts up to 7 days, then AWS may restart it."
  exit 0
fi

[[ -f "$state_file" ]] || die "missing pause state: $state_file"

python3 - "$state_file" <<'PY' | while IFS=$'\t' read -r kind a b c d; do
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    state = json.load(f)

for asg in state["autoscaling_groups"]:
    print("asg", asg["name"], " ".join(asg.get("instance_ids", [])), "", "", sep="\t")
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
      if [[ -n "$b" ]]; then
        read -r -a ids <<< "$b"
        echo "starting Auto Scaling group EC2 instances: $a"
        aws ec2 start-instances --region "$region" --instance-ids "${ids[@]}" >/dev/null
        aws ec2 wait instance-running --region "$region" --instance-ids "${ids[@]}"
        aws ec2 wait instance-status-ok --region "$region" --instance-ids "${ids[@]}"
        for instance_id in "${ids[@]}"; do
          aws autoscaling set-instance-health \
            --region "$region" \
            --auto-scaling-group-name "$a" \
            --instance-id "$instance_id" \
            --health-status Healthy
        done
      fi
      echo "resuming Auto Scaling group processes: $a"
      aws autoscaling resume-processes --region "$region" --auto-scaling-group-name "$a"
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

python3 - "$state_file" <<'PY' | while IFS=$'\t' read -r asg desired; do
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    state = json.load(f)
for group in state["autoscaling_groups"]:
    print(group["name"], group["desired_capacity"], sep="\t")
PY
  (( desired > 0 )) || continue
  echo "waiting for Auto Scaling group to resume: $asg"
  for _ in {1..60}; do
    in_service="$(aws autoscaling describe-auto-scaling-groups \
      --region "$region" \
      --auto-scaling-group-names "$asg" \
      --query 'length(AutoScalingGroups[0].Instances[?LifecycleState==`InService`])' \
      --output text)"
    (( in_service >= desired )) && break
    sleep 10
  done
  (( in_service >= desired )) || die "Auto Scaling group did not resume within 10 minutes: $asg"
done

python3 - "$state_file" <<'PY' | while IFS=$'\t' read -r cluster service desired; do
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    state = json.load(f)
for service in state["ecs_services"]:
    print(service["cluster"], service["name"], service["desired_count"], sep="\t")
PY
  (( desired > 0 )) || continue
  echo "waiting for ECS container instance: $cluster"
  for _ in {1..60}; do
    container_instances="$(aws ecs list-container-instances \
      --region "$region" \
      --cluster "$cluster" \
      --query 'length(containerInstanceArns)' \
      --output text)"
    (( container_instances > 0 )) && break
    sleep 10
  done
  (( container_instances > 0 )) || die "ECS container instance did not register within 10 minutes: $cluster"

  echo "redeploying ECS service after resume: $service"
  aws ecs update-service \
    --region "$region" \
    --cluster "$cluster" \
    --service "$service" \
    --force-new-deployment >/dev/null
  aws ecs wait services-stable --region "$region" --cluster "$cluster" --services "$service"
done

echo "resume completed."
