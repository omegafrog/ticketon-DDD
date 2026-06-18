variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Name prefix for AWS resources."
  type        = string
  default     = "ticketon"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "dev"
}

variable "instance_type" {
  description = "EC2 instance type for the ECS container instance. t3.small is the practical minimum for this multi-container Java demo shape."
  type        = string
  default     = "t3.small"
}

variable "ecs_optimized_ami_ssm_path" {
  description = "SSM parameter path for the latest ECS-optimized AMI."
  type        = string
  default     = "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended/image_id"
}

variable "root_volume_size_gb" {
  description = "Root EBS volume size. 30 GB aligns with the classic EC2 free tier EBS allowance."
  type        = number
  default     = 30
}

variable "swap_size_gb" {
  description = "Swap file size on the ECS container instance. Helps tiny free-tier instances survive Java container bursts."
  type        = number
  default     = 2
}

variable "associate_public_ip_address" {
  description = "Attach a public IPv4 address to the ECS instance. Needed for direct browser access without ALB."
  type        = bool
  default     = true
}

variable "allowed_http_cidrs" {
  description = "CIDR blocks allowed to reach the gateway on port 8080."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "allowed_ssh_cidrs" {
  description = "CIDR blocks allowed to SSH to the instance. Leave empty to disable SSH ingress."
  type        = list(string)
  default     = []
}

variable "key_name" {
  description = "Optional EC2 key pair name for SSH access."
  type        = string
  default     = null
}

variable "image_tag" {
  description = "Default ECR image tag used by all Ticketon application containers."
  type        = string
  default     = "latest"
}

variable "enable_ecs_service" {
  description = "Create the ECS service. Keep false for the first apply so ECR repositories can be created before images are pushed."
  type        = bool
  default     = false
}

variable "service_image_tags" {
  description = "Optional per-service image tag overrides."
  type        = map(string)
  default     = {}
}

variable "db_name" {
  description = "MySQL database name shared by the free-tier deployment."
  type        = string
  default     = "ticketon"
}

variable "db_host" {
  description = "RDS MySQL endpoint hostname used by application services."
  type        = string
}

variable "db_port" {
  description = "RDS MySQL port."
  type        = number
  default     = 3306
}

variable "db_username" {
  description = "MySQL application user."
  type        = string
  default     = "ticketon"
}

variable "db_password" {
  description = "MySQL application user password. This is stored in Terraform state."
  type        = string
  sensitive   = true
}

variable "rabbitmq_username" {
  description = "RabbitMQ default user."
  type        = string
  default     = "ticketon"
}

variable "rabbitmq_password" {
  description = "RabbitMQ default user password. This is stored in Terraform state."
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret for services that validate or issue tokens. This is stored in Terraform state."
  type        = string
  sensitive   = true
  default     = ""
}

variable "jwt_expiration" {
  description = "JWT expiration in milliseconds."
  type        = string
  default     = "3600000"
}

variable "password_secret" {
  description = "Password encoder secret for auth. This is stored in Terraform state."
  type        = string
  sensitive   = true
  default     = ""
}

variable "timezone" {
  description = "Container timezone."
  type        = string
  default     = "Asia/Seoul"
}

variable "java_tool_options" {
  description = "Per-service JVM memory options tuned for a tiny EC2 instance."
  type        = map(string)
  default = {
    eureka     = "-Xms48m -Xmx96m"
    gateway    = "-Xms64m -Xmx160m"
    app        = "-Xms96m -Xmx224m"
    auth       = "-Xms64m -Xmx160m"
    broker     = "-Xms48m -Xmx112m"
    dispatcher = "-Xms48m -Xmx112m"
  }
}

variable "container_memory_reservation" {
  description = "Soft memory reservations in MiB. Keep the total low for t2.micro/t3.micro scheduling."
  type        = map(number)
  default = {
    redis      = 16
    polling    = 16
    rabbitmq   = 96
    eureka     = 64
    app        = 128
    auth       = 96
    broker     = 64
    dispatcher = 64
    gateway    = 96
  }
}

variable "service_environment" {
  description = "Additional environment variables by application service name. Do not put production secrets here unless you accept Terraform state exposure."
  type        = map(map(string))
  default     = {}
}

variable "gateway_cors_allowed_origin_patterns" {
  description = "Comma-separated CORS allowed origin patterns for the gateway."
  type        = string
  default     = ""
}

variable "github_repository" {
  description = "GitHub repository allowed to assume the deployment role, in owner/repo form. Leave null to skip creating the GitHub Actions OIDC role."
  type        = string
  default     = null
}

variable "github_oidc_provider_arn" {
  description = "Existing GitHub Actions OIDC provider ARN. Leave null to let Terraform create one."
  type        = string
  default     = null
}

variable "github_oidc_thumbprints" {
  description = "Thumbprints for the GitHub Actions OIDC provider."
  type        = list(string)
  default     = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}
