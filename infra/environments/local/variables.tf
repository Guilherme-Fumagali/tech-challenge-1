variable "cluster_name" {
  description = "Nome do cluster kind"
  type        = string
  default     = "oficina-local"
}

variable "k8s_manifests_path" {
  description = "Caminho para os manifests Kubernetes (relativo a este módulo)"
  type        = string
  default     = "../../../k8s"
}
