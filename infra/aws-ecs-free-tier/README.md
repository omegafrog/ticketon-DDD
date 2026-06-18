# AWS ECS Free-Tier Terraform

This Terraform stack deploys Ticketon on one ECS-on-EC2 container instance:

- one free-tier-sized EC2 instance in a public subnet
- one ECS task with Gateway, Eureka, App, Auth, Broker, Dispatcher
- Redis, polling Redis, and RabbitMQ as sidecar containers on the same instance
- external RDS MySQL reached through `db_host` and `db_port`
- ECR repositories for application images
- no ALB, NAT Gateway, or ElastiCache

The cost-saving tradeoff is intentional: this is a small demo deployment shape, not production HA. A single micro instance may be tight for all Java services, so increase `instance_type` if containers stop with OOM errors.

## Cost Notes

ECS EC2 launch type has no additional ECS charge, but the underlying resources can still bill: EC2, EBS, public IPv4, ECR storage, and data transfer. Newer AWS Free Tier accounts use credits, and older 12-month free-tier accounts have different service allowances. Confirm the account's Billing Free Tier page before applying.

## Build and Push Images

### One-command backend deploy

From the repository root:

```bash
./infra/aws-ecs-free-tier/deploy-backend.sh
```

The script:

- initializes Terraform
- creates/updates ECR repositories
- builds backend runtime JARs
- builds and pushes backend Docker images
- applies Terraform with the ECS service enabled
- forces a fresh ECS deployment
- waits for ECS stability
- prints the public Gateway URL
- optionally dispatches the frontend GitHub deploy workflow with the Gateway URL

Frontend workflow dispatch is disabled by default. Enable it with:

```bash
DEPLOY_FRONTEND=true \
FRONTEND_DISPATCH_TOKEN=<github-token-with-actions-write> \
FRONTEND_REPOSITORY=omegafrog/ticketon \
FRONTEND_WORKFLOW=deploy.yml \
FRONTEND_REF=main \
./infra/aws-ecs-free-tier/deploy-backend.sh
```

The script dispatches the frontend workflow with:

- `backend_main_url`
- `backend_auth_url`
- `backend_queue_url`

Before first run, copy and edit `terraform.tfvars` or provide secret variables through the environment:

```bash
cp infra/aws-ecs-free-tier/terraform.tfvars.example infra/aws-ecs-free-tier/terraform.tfvars
```

Required values are `db_host`, `db_password`, and `rabbitmq_password`.
Set the optional `sns_google_*`, `sns_kakao_*`, and `cookie_domain` values if social login must work in the deployed environment.
If `terraform.tfvars` exists, replace placeholder values in that file; Terraform loads it before `TF_VAR_*` environment defaults.

Optional script settings:

```bash
IMAGE_TAG=2026-06-17-1 AUTO_APPROVE=true WAIT_HTTP=true ./infra/aws-ecs-free-tier/deploy-backend.sh
```

`SEED_SAMPLE_DATA` defaults to `false`. The ECS stack does not create a MySQL container, so seed [sample-data.sql](sample-data.sql) directly into RDS with a MySQL client if sample data is needed.

### Manual flow

Create the infra once to get ECR URLs:

```bash
cd infra/aws-ecs-free-tier
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform apply
terraform output -json ecr_repositories
```

Keep `enable_ecs_service = false` for this first apply. That avoids an ECS deployment failing before the application images exist.

Build the jars and runtime images from the repository root:

```bash
./gradlew :platform:eureka:build :platform:gateway:build :app:build :auth:build :broker:build :dispatcher:build -x test
```

Then authenticate Docker to ECR and push each image. Example:

```bash
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com

docker build -f platform/eureka/Dockerfile.runtime -t <eureka-repo-url>:latest platform/eureka
docker push <eureka-repo-url>:latest
```

Repeat with these Dockerfiles and repositories:

- `platform/gateway/Dockerfile.runtime` -> `gateway`
- `app/Dockerfile.runtime` -> `app`
- `auth/Dockerfile.runtime` -> `auth`
- `broker/Dockerfile.runtime` -> `broker`
- `dispatcher/Dockerfile.runtime` -> `dispatcher`

After pushing images:

```bash
terraform apply -var="enable_ecs_service=true"
aws ecs update-service \
  --cluster "$(terraform output -raw cluster_name)" \
  --service "$(terraform output -raw service_name)" \
  --force-new-deployment
```

## GitHub Actions + CodeDeploy

This stack also creates:

- a CodeDeploy EC2/Server application and deployment group
- an S3 bucket for CodeDeploy bundles
- a CodeDeploy agent on the ECS container instance
- an optional GitHub Actions OIDC IAM role when `github_repository` is set

Why EC2/Server CodeDeploy instead of ECS blue/green: ECS blue/green requires load balancer traffic control, which adds cost and does not match this free-tier-oriented single-instance deployment.

Set `github_repository` in `terraform.tfvars`:

```hcl
github_repository = "owner/repo"
```

Apply Terraform and read the workflow values:

```bash
terraform apply
terraform output github_actions_role_arn
terraform output codedeploy_application_name
terraform output codedeploy_deployment_group_name
terraform output codedeploy_s3_bucket
```

In GitHub repository variables, set:

```text
AWS_ROLE_TO_ASSUME=<github_actions_role_arn>
AWS_REGION=ap-northeast-2
PROJECT_NAME=ticketon
DEPLOY_ENV=dev
```

The workflow `.github/workflows/aws-ecs-codedeploy.yml` builds the runtime images, pushes them to ECR, uploads the CodeDeploy bundle to S3, and creates a CodeDeploy deployment. The CodeDeploy hook on the EC2 instance forces the ECS service to redeploy and waits until it is stable.

The same workflow runs on `main` push. During deployment it:

- resolves the current Vercel production frontend origins and injects them into the Gateway ECS task through `GATEWAY_CORS_ALLOWED_ORIGIN_PATTERNS`
- registers a fresh ECS task definition using the current commit SHA image tags
- optionally dispatches the frontend deploy workflow with the resolved public Gateway URL, so the frontend build knows the backend domain/IP

To enable frontend origin resolution, set these backend repository secrets/variables:

```text
VERCEL_TOKEN=<vercel-token>
VERCEL_PROJECT_ID=<frontend-vercel-project-id>
VERCEL_TEAM_ID=<optional-vercel-team-id>
VERCEL_PROJECT_NAME=ticketon
```

To enable frontend workflow dispatch from backend deploy, set:

```text
FRONTEND_REPOSITORY=owner/ticketon
FRONTEND_WORKFLOW=deploy.yml
FRONTEND_REF=main
FRONTEND_DISPATCH_TOKEN=<github-token-with-actions-write> # secret, optional if GITHUB_TOKEN can dispatch target workflow
```

Before the first workflow deployment, make sure the ECS service exists:

```bash
terraform apply -var="enable_ecs_service=true"
```

## Runtime Access

The gateway listens on port `8080`. Find the EC2 public DNS/IP in the AWS console or with:

```bash
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=ticketon-dev-ecs" "Name=instance-state-name,Values=running" \
  --query "Reservations[].Instances[].PublicDnsName" \
  --output text
```

Then open:

```text
http://<public-dns>:8080
```

## Secrets

`db_password`, `rabbitmq_password`, JWT/password secrets, OAuth client secrets, and any values placed in `service_environment` are stored in Terraform state. For production, move secrets to AWS Secrets Manager or SSM Parameter Store and wire ECS `secrets` instead.

## Teardown

```bash
terraform destroy
```

ECR repositories are configured with `force_delete = true`, so images are deleted during destroy.
