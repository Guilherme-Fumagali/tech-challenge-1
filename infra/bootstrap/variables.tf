variable "aws_region" {
  description = "Região onde o bucket S3 e a tabela DynamoDB serão criados"
  type        = string
  default     = "us-east-1"
}

variable "bucket_name" {
  description = "Nome do bucket S3 para o state do ambiente aws (deve ser globalmente único)"
  type        = string
  default     = "oficina-api-tfstate"
}

variable "lock_table_name" {
  description = "Nome da tabela DynamoDB usada para lock de state"
  type        = string
  default     = "oficina-api-tfstate-lock"
}
