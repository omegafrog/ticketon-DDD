resource "aws_ecr_repository" "service" {
  for_each = local.app_services

  name                 = "${local.name}/${each.key}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  force_delete = true
}

resource "aws_ecr_lifecycle_policy" "service" {
  for_each   = local.app_services
  repository = aws_ecr_repository.service[each.key].name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the latest 5 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = {
        type = "expire"
      }
    }]
  })
}
