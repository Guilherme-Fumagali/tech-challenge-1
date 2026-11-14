resource "aws_ssm_parameter" "db_endpoint" {
  name        = "/oficina/${var.ambiente}/os/db-endpoint"
  description = "Endereco host:porta do RDS do OS Service"
  type        = "String"
  value       = aws_db_instance.os.endpoint
}

resource "aws_ssm_parameter" "db_name" {
  name  = "/oficina/${var.ambiente}/os/db-name"
  type  = "String"
  value = var.db_name
}

resource "aws_ssm_parameter" "db_username" {
  name  = "/oficina/${var.ambiente}/os/db-username"
  type  = "String"
  value = var.db_username
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/oficina/${var.ambiente}/os/db-password"
  type  = "SecureString"
  value = random_password.db.result
}
