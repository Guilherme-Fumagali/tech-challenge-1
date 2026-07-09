output "eks_cluster_name" {
  value = aws_eks_cluster.oficina.name
}

output "eks_cluster_endpoint" {
  value = aws_eks_cluster.oficina.endpoint
}

output "db_endpoint" {
  value     = aws_db_instance.oficina.endpoint
  sensitive = true
}

output "estimated_hourly_cost_usd" {
  description = "Estimativa informativa — ver detalhamento em infra/README.md"
  value       = "~0.16/h (EKS control plane 0.10 + 2x t3.small ~0.042 + db.t4g.micro ~0.016)"
}
