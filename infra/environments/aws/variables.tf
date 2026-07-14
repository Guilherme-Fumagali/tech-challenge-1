variable "aws_region" {
  description = "Região AWS onde tudo será provisionado"
  type        = string
  default     = "us-east-1"
}

variable "cluster_name" {
  description = "Nome do cluster EKS"
  type        = string
  default     = "oficina-api-aws"
}

variable "kubernetes_version" {
  description = "Versão do Kubernetes no EKS"
  type        = string
  default     = "1.33"
}

variable "node_instance_types" {
  description = "Tipos de instância EC2 para os nós do EKS (t3.small = menor custo com folga pra kubelet + app)"
  type        = list(string)
  default     = ["t3.small"]
}

variable "cluster_admin_principal_arn" {
  description = "ARN do principal IAM que recebe admin do cluster via access entry (kubectl/console). Vazio desliga."
  type        = string
  default     = "arn:aws:iam::002754693932:user/gfumagali-admin"
}

variable "public_subnet_cidrs" {
  description = "CIDRs das subnets públicas por AZ. Acoplado à região default (us-east-1) — ajuste as chaves de AZ se trocar aws_region."
  type        = map(string)
  default = {
    "us-east-1a" = "10.0.1.0/24"
    "us-east-1b" = "10.0.2.0/24"
  }
}

variable "db_instance_class" {
  description = "Classe da instância RDS (db.t4g.micro = menor custo, elegível a free tier em conta nova)"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_name" {
  description = "Nome do banco Postgres"
  type        = string
  default     = "oficina"
}

variable "db_username" {
  description = "Usuário administrador do RDS"
  type        = string
  default     = "oficina"
  sensitive   = true
}

variable "db_password" {
  description = "Senha do RDS — sem default proposital, defina via terraform.tfvars (gitignorado) ou TF_VAR_db_password"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Chave de assinatura JWT (mínimo 32 caracteres) — sem default proposital"
  type        = string
  sensitive   = true
}

variable "admin_password" {
  description = "Senha do usuário admin da aplicação"
  type        = string
  sensitive   = true
  default     = "admin123"
}

variable "k8s_manifests_path" {
  description = "Caminho para os manifests Kubernetes (relativo a este módulo)"
  type        = string
  default     = "../../../k8s"
}
