output "fila_os_eventos_url" {
  description = "URL da fila de eventos consumida pelo OS Service."
  value       = aws_sqs_queue.os_eventos.url
}

output "fila_os_eventos_arn" {
  description = "ARN da fila de eventos consumida pelo OS Service."
  value       = aws_sqs_queue.os_eventos.arn
}

output "fila_os_eventos_dlq_url" {
  description = "URL da DLQ da fila de eventos."
  value       = aws_sqs_queue.os_eventos_dlq.url
}

output "role_pod_arn" {
  description = "ARN da role associada ao ServiceAccount os-service por Pod Identity."
  value       = aws_iam_role.pod.arn
}

output "db_endpoint" {
  description = "Endereco do RDS do OS Service, publicado tambem no SSM."
  value       = aws_db_instance.os.endpoint
}

output "db_security_group_id" {
  description = "Security group do RDS do OS Service."
  value       = aws_security_group.rds.id
}
