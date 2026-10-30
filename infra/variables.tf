variable "aws_region" {
  description = "Região da AWS."
  type        = string
  default     = "us-east-1"
}

variable "ambiente" {
  description = "Ambiente lógico (staging ou prod)."
  type        = string
  default     = "staging"

  validation {
    condition     = contains(["staging", "prod"], var.ambiente)
    error_message = "ambiente deve ser staging ou prod."
  }
}

variable "db_instance_class" {
  description = "Classe da instância RDS do OS Service."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Armazenamento do RDS em GB."
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Nome do banco criado na instância."
  type        = string
  default     = "oficina"
}

variable "db_username" {
  description = "Usuário administrador do banco."
  type        = string
  default     = "oficina"
}

variable "engine_version" {
  description = "Versão do PostgreSQL."
  type        = string
  default     = "16"
}

variable "namespace_kubernetes" {
  description = "Namespace do Deployment do OS Service no EKS."
  type        = string
  default     = "oficina"
}

variable "service_account" {
  description = "ServiceAccount do Deployment do OS Service, associada à role de pod."
  type        = string
  default     = "os-service"
}
