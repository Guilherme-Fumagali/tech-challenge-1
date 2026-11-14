data "aws_iam_policy_document" "confianca_pod_identity" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole", "sts:TagSession"]

    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "pod" {
  name                 = "oficina-os-pod-${var.ambiente}"
  description          = "Role do pod do OS Service via EKS Pod Identity"
  assume_role_policy   = data.aws_iam_policy_document.confianca_pod_identity.json
  permissions_boundary = "arn:aws:iam::${local.conta}:policy/oficina-pod-boundary"
}

data "aws_iam_policy_document" "mensageria" {
  statement {
    sid    = "ConsumirFilaDeEventos"
    effect = "Allow"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
    ]
    resources = [aws_sqs_queue.os_eventos.arn]
  }

  statement {
    sid       = "MonitorarDlq"
    effect    = "Allow"
    actions   = ["sqs:GetQueueUrl", "sqs:GetQueueAttributes"]
    resources = [aws_sqs_queue.os_eventos_dlq.arn]
  }

  statement {
    sid       = "EnviarComandosAosParticipantes"
    effect    = "Allow"
    actions   = ["sqs:SendMessage", "sqs:GetQueueUrl", "sqs:GetQueueAttributes"]
    resources = local.filas_destino
  }
}

resource "aws_iam_role_policy" "mensageria" {
  name   = "mensageria"
  role   = aws_iam_role.pod.id
  policy = data.aws_iam_policy_document.mensageria.json
}

resource "aws_eks_pod_identity_association" "os_service" {
  cluster_name    = data.aws_ssm_parameter.eks_cluster_name.value
  namespace       = var.namespace_kubernetes
  service_account = var.service_account
  role_arn        = aws_iam_role.pod.arn
}
