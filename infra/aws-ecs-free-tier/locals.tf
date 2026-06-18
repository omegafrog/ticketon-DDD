data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_ssm_parameter" "ecs_optimized_ami" {
  name = var.ecs_optimized_ami_ssm_path
}

locals {
  name = "${var.project_name}-${var.environment}"

  codedeploy_bucket_name = "${local.name}-codedeploy-${data.aws_caller_identity.current.account_id}-${data.aws_region.current.name}"

  github_oidc_enabled = var.github_repository != null && var.github_repository != ""
  github_oidc_provider_arn = var.github_oidc_provider_arn != null ? var.github_oidc_provider_arn : (
    local.github_oidc_enabled ? aws_iam_openid_connect_provider.github[0].arn : null
  )

  app_services = toset([
    "gateway",
    "eureka",
    "app",
    "auth",
    "broker",
    "dispatcher",
  ])

  image_tags = {
    for service in local.app_services :
    service => lookup(var.service_image_tags, service, var.image_tag)
  }

  images = {
    for service in local.app_services :
    service => "${aws_ecr_repository.service[service].repository_url}:${local.image_tags[service]}"
  }

  rds_jdbc_url = "jdbc:mysql://${var.db_host}:${var.db_port}/${var.db_name}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"

  base_java_environment = [
    { name = "TZ", value = var.timezone },
    { name = "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", value = "http://eureka:8761/eureka/" },
    { name = "SPRING_RABBITMQ_HOST", value = "rabbitmq" },
    { name = "SPRING_RABBITMQ_PORT", value = "5672" },
    { name = "SPRING_RABBITMQ_USERNAME", value = var.rabbitmq_username },
    { name = "SPRING_RABBITMQ_PASSWORD", value = var.rabbitmq_password },
  ]

  db_environment = [
    { name = "SPRING_DATASOURCE_PRIMARY_URL", value = local.rds_jdbc_url },
    { name = "SPRING_DATASOURCE_PRIMARY_JDBC_URL", value = local.rds_jdbc_url },
    { name = "SPRING_DATASOURCE_PRIMARY_USERNAME", value = var.db_username },
    { name = "SPRING_DATASOURCE_PRIMARY_PASSWORD", value = var.db_password },
    { name = "SPRING_DATASOURCE_PRIMARY_DRIVER_CLASS_NAME", value = "com.mysql.cj.jdbc.Driver" },
    { name = "SPRING_DATASOURCE_READONLY_URL", value = local.rds_jdbc_url },
    { name = "SPRING_DATASOURCE_READONLY_JDBC_URL", value = local.rds_jdbc_url },
    { name = "SPRING_DATASOURCE_READONLY_USERNAME", value = var.db_username },
    { name = "SPRING_DATASOURCE_READONLY_PASSWORD", value = var.db_password },
    { name = "SPRING_DATASOURCE_READONLY_DRIVER_CLASS_NAME", value = "com.mysql.cj.jdbc.Driver" },
  ]

  service_extra_environment = {
    for service, env in var.service_environment :
    service => [for name, value in env : { name = name, value = value }]
  }

  gateway_cors_environment = var.gateway_cors_allowed_origin_patterns != "" ? [
    { name = "GATEWAY_CORS_ALLOWED_ORIGIN_PATTERNS", value = var.gateway_cors_allowed_origin_patterns },
  ] : []

  jwt_environment = var.jwt_secret != "" ? [
    { name = "CUSTOM_JWT_SECRET", value = var.jwt_secret },
    { name = "CUSTOM_JWT_EXPIRATION", value = var.jwt_expiration },
  ] : []

  auth_secret_environment = var.password_secret != "" ? [
    { name = "CUSTOM_PASSWORD_SECRET", value = var.password_secret },
  ] : []

  auth_oauth_environment = [
    { name = "CUSTOM_COOKIE_DOMAIN", value = var.cookie_domain },
    { name = "SNS_GOOGLE_URL", value = var.sns_google_url },
    { name = "SNS_GOOGLE_CLIENT_ID", value = var.sns_google_client_id },
    { name = "SNS_GOOGLE_CALLBACK_URL", value = var.sns_google_callback_url },
    { name = "SNS_GOOGLE_CLIENT_SECRET", value = var.sns_google_client_secret },
    { name = "SNS_GOOGLE_TOKEN_URL", value = var.sns_google_token_url },
    { name = "SNS_KAKAO_CLIENT_ID", value = var.sns_kakao_client_id },
    { name = "SNS_KAKAO_CALLBACK_URL", value = var.sns_kakao_callback_url },
    { name = "SNS_KAKAO_TOKEN_URL", value = var.sns_kakao_token_url },
  ]

  container_definitions = [
    {
      name              = "redis"
      image             = "redis:alpine"
      essential         = true
      memoryReservation = var.container_memory_reservation["redis"]
      command           = ["redis-server", "--save", "60", "1", "--loglevel", "warning"]
      portMappings      = [{ containerPort = 6379, protocol = "tcp" }]
      mountPoints       = [{ sourceVolume = "redis-data", containerPath = "/data", readOnly = false }]
      healthCheck = {
        command     = ["CMD-SHELL", "redis-cli ping | grep PONG"]
        interval    = 30
        timeout     = 5
        retries     = 5
        startPeriod = 10
      }
    },
    {
      name              = "polling"
      image             = "redis:alpine"
      essential         = true
      memoryReservation = var.container_memory_reservation["polling"]
      command           = ["redis-server", "--save", "60", "1", "--loglevel", "warning"]
      portMappings      = [{ containerPort = 6379, protocol = "tcp" }]
      mountPoints       = [{ sourceVolume = "polling-data", containerPath = "/data", readOnly = false }]
      healthCheck = {
        command     = ["CMD-SHELL", "redis-cli ping | grep PONG"]
        interval    = 30
        timeout     = 5
        retries     = 5
        startPeriod = 10
      }
    },
    {
      name              = "rabbitmq"
      image             = "rabbitmq:management-alpine"
      essential         = true
      memoryReservation = var.container_memory_reservation["rabbitmq"]
      environment = [
        { name = "RABBITMQ_DEFAULT_USER", value = var.rabbitmq_username },
        { name = "RABBITMQ_DEFAULT_PASS", value = var.rabbitmq_password },
      ]
      portMappings = [
        { containerPort = 5672, protocol = "tcp" },
        { containerPort = 15672, protocol = "tcp" },
      ]
      mountPoints = [{ sourceVolume = "rabbitmq-data", containerPath = "/var/lib/rabbitmq", readOnly = false }]
      healthCheck = {
        command     = ["CMD-SHELL", "rabbitmq-diagnostics -q ping"]
        interval    = 30
        timeout     = 10
        retries     = 10
        startPeriod = 45
      }
    },
    {
      name              = "eureka"
      image             = local.images["eureka"]
      essential         = true
      memoryReservation = var.container_memory_reservation["eureka"]
      environment = concat([
        { name = "TZ", value = var.timezone },
        { name = "JAVA_TOOL_OPTIONS", value = var.java_tool_options["eureka"] },
      ], lookup(local.service_extra_environment, "eureka", []))
      portMappings = [{ containerPort = 8761, protocol = "tcp" }]
      links        = ["redis", "rabbitmq"]
      dependsOn = [
        { containerName = "redis", condition = "HEALTHY" },
        { containerName = "rabbitmq", condition = "HEALTHY" },
      ]
    },
    {
      name              = "app"
      image             = local.images["app"]
      essential         = true
      memoryReservation = var.container_memory_reservation["app"]
      environment = concat(local.base_java_environment, local.db_environment, local.jwt_environment, [
        { name = "JAVA_TOOL_OPTIONS", value = var.java_tool_options["app"] },
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "REDIS_HOST", value = "redis" },
        { name = "REDIS_PORT", value = "6379" },
        { name = "EUREKA_INSTANCE_HOST_NAME", value = "app" },
        { name = "EUREKA_INSTANCE_HOSTNAME", value = "app" },
        { name = "SERVICES_EVENT_BASE_URL", value = "http://app:9000" },
        { name = "SERVICES_SEAT_BASE_URL", value = "http://app:9000" },
      ], lookup(local.service_extra_environment, "app", []))
      portMappings = [{ containerPort = 9000, protocol = "tcp" }]
      links        = ["redis", "rabbitmq", "eureka"]
      dependsOn = [
        { containerName = "redis", condition = "HEALTHY" },
        { containerName = "rabbitmq", condition = "HEALTHY" },
        { containerName = "eureka", condition = "START" },
      ]
    },
    {
      name              = "auth"
      image             = local.images["auth"]
      essential         = true
      memoryReservation = var.container_memory_reservation["auth"]
      environment = concat(local.base_java_environment, local.db_environment, local.jwt_environment, local.auth_secret_environment, local.auth_oauth_environment, [
        { name = "JAVA_TOOL_OPTIONS", value = var.java_tool_options["auth"] },
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "REDIS_HOST", value = "redis" },
        { name = "REDIS_PORT", value = "6379" },
        { name = "EUREKA_INSTANCE_HOST_NAME", value = "auth" },
        { name = "EUREKA_INSTANCE_HOSTNAME", value = "auth" },
        { name = "SERVICE_USER_BASE_URL", value = "http://app:9000" },
      ], lookup(local.service_extra_environment, "auth", []))
      portMappings = [{ containerPort = 9001, protocol = "tcp" }]
      links        = ["redis", "rabbitmq", "eureka", "app"]
      dependsOn = [
        { containerName = "redis", condition = "HEALTHY" },
        { containerName = "rabbitmq", condition = "HEALTHY" },
        { containerName = "eureka", condition = "START" },
        { containerName = "app", condition = "START" },
      ]
    },
    {
      name              = "broker"
      image             = local.images["broker"]
      essential         = true
      memoryReservation = var.container_memory_reservation["broker"]
      environment = concat(local.base_java_environment, local.jwt_environment, [
        { name = "JAVA_TOOL_OPTIONS", value = var.java_tool_options["broker"] },
        { name = "SERVER_PORT", value = "9003" },
        { name = "REDIS_HOST", value = "polling" },
        { name = "REDIS_PORT", value = "6379" },
        { name = "EUREKA_CLIENT_HOST_NAME", value = "broker" },
        { name = "EUREKA_INSTANCE_HOST_NAME", value = "broker" },
        { name = "EUREKA_INSTANCE_HOSTNAME", value = "broker" },
        { name = "EVENT_SERVICE_BASE_URL", value = "http://app:9000" },
        { name = "CUSTOM_EVENTS_URL", value = "http://app:9000" },
      ], lookup(local.service_extra_environment, "broker", []))
      portMappings = [{ containerPort = 9003, protocol = "tcp" }]
      links        = ["polling", "eureka", "app"]
      dependsOn = [
        { containerName = "polling", condition = "HEALTHY" },
        { containerName = "eureka", condition = "START" },
        { containerName = "app", condition = "START" },
      ]
    },
    {
      name              = "dispatcher"
      image             = local.images["dispatcher"]
      essential         = true
      memoryReservation = var.container_memory_reservation["dispatcher"]
      environment = concat([
        { name = "TZ", value = var.timezone },
        { name = "JAVA_TOOL_OPTIONS", value = var.java_tool_options["dispatcher"] },
        { name = "REDIS_HOST", value = "polling" },
        { name = "REDIS_PORT", value = "6379" },
        { name = "SPRING_DATA_REDIS_HOST", value = "polling" },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" },
      ], lookup(local.service_extra_environment, "dispatcher", []))
      portMappings = [{ containerPort = 9002, protocol = "tcp" }]
      links        = ["polling"]
      dependsOn    = [{ containerName = "polling", condition = "HEALTHY" }]
    },
    {
      name              = "gateway"
      image             = local.images["gateway"]
      essential         = true
      memoryReservation = var.container_memory_reservation["gateway"]
      environment = concat(local.jwt_environment, [
        { name = "TZ", value = var.timezone },
        { name = "JAVA_TOOL_OPTIONS", value = var.java_tool_options["gateway"] },
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "REDIS_HOST", value = "redis" },
        { name = "REDIS_PORT", value = "6379" },
        { name = "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", value = "http://eureka:8761/eureka/" },
        { name = "EUREKA_CLIENT_HOST_NAME", value = "gateway" },
        { name = "EUREKA_HOST_NAME", value = "gateway" },
        { name = "EUREKA_INSTANCE_HOSTNAME", value = "gateway" },
      ], local.gateway_cors_environment, lookup(local.service_extra_environment, "gateway", []))
      portMappings = [{ containerPort = 8080, hostPort = 8080, protocol = "tcp" }]
      links        = ["redis", "eureka", "app", "auth", "broker"]
      dependsOn = [
        { containerName = "redis", condition = "HEALTHY" },
        { containerName = "eureka", condition = "START" },
        { containerName = "app", condition = "START" },
        { containerName = "auth", condition = "START" },
        { containerName = "broker", condition = "START" },
      ]
    },
  ]
}
