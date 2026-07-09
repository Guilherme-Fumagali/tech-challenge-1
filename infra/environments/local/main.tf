terraform {
  required_version = ">= 1.5"
  required_providers {
    kind = {
      source  = "tehcyx/kind"
      version = "~> 0.11"
    }
    null = {
      source  = "hashicorp/null"
      version = "~> 3.2"
    }
  }
}

provider "kind" {}

resource "kind_cluster" "oficina" {
  name            = var.cluster_name
  wait_for_ready  = true
  kubeconfig_path = "${path.module}/kubeconfig"
}

# Aplica os manifests via kubectl (local-exec) em vez do provider kubernetes/kubectl —
# evita o problema de "ovo e galinha": o provider precisaria de um cluster alcançável
# em tempo de plan, mas o cluster só existe após este mesmo apply.
resource "null_resource" "apply_manifests" {
  depends_on = [kind_cluster.oficina]

  triggers = {
    manifests_hash = sha1(join("", [for f in fileset(var.k8s_manifests_path, "**/*.yaml") : filesha1("${var.k8s_manifests_path}/${f}")]))
  }

  provisioner "local-exec" {
    # Comando em uma linha só (join com &&) para funcionar tanto no cmd.exe (Windows,
    # dev local) quanto no sh (Linux, runners do GitHub Actions) sem depender de interpreter.
    command = "kubectl --kubeconfig=${kind_cluster.oficina.kubeconfig_path} apply -f ${var.k8s_manifests_path}/namespace.yaml && kubectl --kubeconfig=${kind_cluster.oficina.kubeconfig_path} apply -f ${var.k8s_manifests_path}/database/ && kubectl --kubeconfig=${kind_cluster.oficina.kubeconfig_path} apply -f ${var.k8s_manifests_path}/mailhog/ && kubectl --kubeconfig=${kind_cluster.oficina.kubeconfig_path} apply -f ${var.k8s_manifests_path}/app/"
  }
}
