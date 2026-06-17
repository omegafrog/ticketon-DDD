output "cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.this.name
}

output "aws_region" {
  description = "AWS region used by this stack."
  value       = var.aws_region
}

output "service_name" {
  description = "ECS service name."
  value       = local.name
}

output "ecr_repositories" {
  description = "ECR repository URLs by service."
  value       = { for name, repo in aws_ecr_repository.service : name => repo.repository_url }
}

output "gateway_port" {
  description = "Public gateway port on the ECS instance."
  value       = 8080
}

output "next_steps" {
  description = "Short deployment reminder."
  value       = "Push images to the ECR repository URLs, then run terraform apply again or force a new ECS deployment."
}

output "codedeploy_application_name" {
  description = "CodeDeploy application name."
  value       = aws_codedeploy_app.ticketon.name
}

output "codedeploy_deployment_group_name" {
  description = "CodeDeploy deployment group name."
  value       = aws_codedeploy_deployment_group.ticketon.deployment_group_name
}

output "codedeploy_s3_bucket" {
  description = "S3 bucket used for CodeDeploy bundles."
  value       = aws_s3_bucket.codedeploy.bucket
}

output "github_actions_role_arn" {
  description = "GitHub Actions OIDC role ARN. Null when github_repository is not set."
  value       = local.github_oidc_enabled ? aws_iam_role.github_actions[0].arn : null
}
