output "kubeconfig_path" {
  description = "Caminho local do kubeconfig gerado para o cluster"
  value       = kind_cluster.oficina.kubeconfig_path
}

output "cluster_name" {
  description = "Nome do cluster kind criado"
  value       = kind_cluster.oficina.name
}
