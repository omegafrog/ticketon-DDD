resource "aws_ecs_cluster" "this" {
  name = local.name
}

resource "aws_launch_template" "ecs" {
  name_prefix   = "${local.name}-ecs-"
  image_id      = data.aws_ssm_parameter.ecs_optimized_ami.value
  instance_type = var.instance_type
  key_name      = var.key_name

  iam_instance_profile {
    name = aws_iam_instance_profile.ecs_instance.name
  }

  network_interfaces {
    associate_public_ip_address = var.associate_public_ip_address
    security_groups             = [aws_security_group.ecs_instance.id]
  }

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_size           = var.root_volume_size_gb
      volume_type           = "gp3"
      delete_on_termination = true
      encrypted             = true
    }
  }

  user_data = base64encode(templatefile("${path.module}/templates/user-data.sh.tftpl", {
    cluster_name      = aws_ecs_cluster.this.name
    service_name      = local.name
    aws_region        = data.aws_region.current.name
    swap_size_gb      = var.swap_size_gb
    eip_allocation_id = aws_eip.gateway.id
  }))

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "${local.name}-ecs"
    }
  }
}

resource "aws_autoscaling_group" "ecs" {
  name                = "${local.name}-ecs"
  min_size            = 1
  max_size            = 1
  desired_capacity    = 1
  vpc_zone_identifier = [aws_subnet.public.id]

  launch_template {
    id      = aws_launch_template.ecs.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "${local.name}-ecs"
    propagate_at_launch = true
  }
}

resource "aws_ecs_capacity_provider" "this" {
  name = "${local.name}-ec2"

  auto_scaling_group_provider {
    auto_scaling_group_arn         = aws_autoscaling_group.ecs.arn
    managed_termination_protection = "DISABLED"

    managed_scaling {
      status                    = "DISABLED"
      target_capacity           = 100
      minimum_scaling_step_size = 1
      maximum_scaling_step_size = 1
    }
  }
}

resource "aws_ecs_cluster_capacity_providers" "this" {
  cluster_name       = aws_ecs_cluster.this.name
  capacity_providers = [aws_ecs_capacity_provider.this.name]

  default_capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.this.name
    weight            = 1
  }
}

resource "aws_ecs_task_definition" "ticketon" {
  family                   = local.name
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn
  container_definitions    = jsonencode(local.container_definitions)

  volume {
    name      = "redis-data"
    host_path = "/ecs/ticketon/redis"
  }

  volume {
    name      = "polling-data"
    host_path = "/ecs/ticketon/polling"
  }

  volume {
    name      = "rabbitmq-data"
    host_path = "/ecs/ticketon/rabbitmq"
  }
}

resource "aws_ecs_service" "ticketon" {
  count = var.enable_ecs_service ? 1 : 0

  name            = local.name
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.ticketon.arn
  desired_count   = 1

  capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.this.name
    weight            = 1
  }

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  depends_on = [
    aws_ecs_cluster_capacity_providers.this,
    aws_iam_role_policy_attachment.task_execution,
  ]
}
