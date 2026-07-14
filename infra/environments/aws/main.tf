terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    null = {
      source  = "hashicorp/null"
      version = "~> 3.2"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.4"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# ── Rede ──────────────────────────────────────────────────────────────────
resource "aws_vpc" "oficina" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name                                          = "oficina-api-vpc"
    "kubernetes.io/cluster/${var.cluster_name}"   = "shared"
  }
}

resource "aws_internet_gateway" "oficina" {
  vpc_id = aws_vpc.oficina.id
  tags   = { Name = "oficina-api-igw" }
}

resource "aws_subnet" "public" {
  for_each = var.public_subnet_cidrs

  vpc_id                  = aws_vpc.oficina.id
  cidr_block               = each.value
  availability_zone        = each.key
  map_public_ip_on_launch  = true

  tags = {
    Name                                         = "oficina-api-public-${each.key}"
    "kubernetes.io/cluster/${var.cluster_name}"  = "shared"
    "kubernetes.io/role/elb"                     = "1"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.oficina.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.oficina.id
  }

  tags = { Name = "oficina-api-public-rt" }
}

resource "aws_route_table_association" "public" {
  for_each       = aws_subnet.public
  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

# ── IAM ───────────────────────────────────────────────────────────────────

data "aws_iam_policy_document" "eks_cluster_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "eks_cluster" {
  name               = "oficina-api-eks-cluster-role"
  assume_role_policy = data.aws_iam_policy_document.eks_cluster_assume.json
}

resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  role       = aws_iam_role.eks_cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

data "aws_iam_policy_document" "eks_node_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "eks_node" {
  name               = "oficina-api-eks-node-role"
  assume_role_policy = data.aws_iam_policy_document.eks_node_assume.json
}

resource "aws_iam_role_policy_attachment" "eks_node_worker" {
  role       = aws_iam_role.eks_node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "eks_node_cni" {
  role       = aws_iam_role.eks_node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "eks_node_ecr_readonly" {
  role       = aws_iam_role.eks_node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# ── EKS ───────────────────────────────────────────────────────────────────

resource "aws_eks_cluster" "oficina" {
  name     = var.cluster_name
  role_arn = aws_iam_role.eks_cluster.arn
  version  = var.kubernetes_version

  access_config {
    authentication_mode = "API_AND_CONFIG_MAP"
  }

  vpc_config {
    subnet_ids              = [for s in aws_subnet.public : s.id]
    endpoint_public_access   = true
    endpoint_private_access  = false
  }

  depends_on = [aws_iam_role_policy_attachment.eks_cluster_policy]
}

# Acesso admin de um principal IAM ao cluster (kubectl/console) via access entry.
resource "aws_eks_access_entry" "admin" {
  count         = var.cluster_admin_principal_arn == "" ? 0 : 1
  cluster_name  = aws_eks_cluster.oficina.name
  principal_arn = var.cluster_admin_principal_arn
}

resource "aws_eks_access_policy_association" "admin" {
  count         = var.cluster_admin_principal_arn == "" ? 0 : 1
  cluster_name  = aws_eks_cluster.oficina.name
  principal_arn = var.cluster_admin_principal_arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [aws_eks_access_entry.admin]
}

# Acesso admin do role assumido pela pipeline CI/CD (kubectl no deploy).
resource "aws_eks_access_entry" "ci" {
  count         = var.ci_role_principal_arn == "" ? 0 : 1
  cluster_name  = aws_eks_cluster.oficina.name
  principal_arn = var.ci_role_principal_arn
}

resource "aws_eks_access_policy_association" "ci" {
  count         = var.ci_role_principal_arn == "" ? 0 : 1
  cluster_name  = aws_eks_cluster.oficina.name
  principal_arn = var.ci_role_principal_arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [aws_eks_access_entry.ci]
}

resource "aws_eks_node_group" "oficina" {
  cluster_name    = aws_eks_cluster.oficina.name
  node_group_name = "oficina-api-nodes"
  node_role_arn   = aws_iam_role.eks_node.arn
  subnet_ids      = [for s in aws_subnet.public : s.id]
  instance_types  = var.node_instance_types

  scaling_config {
    desired_size = 2
    min_size     = 2
    max_size     = 3
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_node_worker,
    aws_iam_role_policy_attachment.eks_node_cni,
    aws_iam_role_policy_attachment.eks_node_ecr_readonly,
  ]
}

# ── RDS ───────────────────────────────────────────────────────────────────

resource "aws_db_subnet_group" "oficina" {
  name       = "oficina-api-db-subnet-group"
  subnet_ids = [for s in aws_subnet.public : s.id]
}

resource "aws_security_group" "rds" {
  name        = "oficina-api-rds-sg"
  description = "Permite Postgres apenas a partir dos nos do EKS"
  vpc_id      = aws_vpc.oficina.id

  ingress {
    description     = "Postgres a partir do cluster EKS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_eks_cluster.oficina.vpc_config[0].cluster_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_instance" "oficina" {
  identifier     = "oficina-api-db"
  engine         = "postgres"
  engine_version = "16"
  instance_class = var.db_instance_class

  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.oficina.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  multi_az             = false
  publicly_accessible  = false
  skip_final_snapshot  = true
  deletion_protection  = false
}

# ── Deploy da aplicação ───────────────────────────────────────────────────────
resource "local_file" "app_configmap" {
  content = templatefile("${path.module}/templates/app-configmap.yaml.tftpl", {
    db_endpoint = aws_db_instance.oficina.endpoint
    db_name     = var.db_name
  })
  filename = "${path.module}/rendered/app-configmap.yaml"
}

resource "local_file" "app_secret" {
  content = templatefile("${path.module}/templates/app-secret.yaml.tftpl", {
    db_username    = var.db_username
    db_password    = var.db_password
    jwt_secret     = var.jwt_secret
    admin_password = var.admin_password
  })
  filename = "${path.module}/rendered/app-secret.yaml"
}

resource "null_resource" "deploy_app" {
  depends_on = [
    aws_eks_node_group.oficina,
    local_file.app_configmap,
    local_file.app_secret,
    aws_eks_access_policy_association.ci,
    aws_eks_access_policy_association.admin,
  ]

  triggers = {
    configmap_hash      = local_file.app_configmap.content_md5
    secret_hash         = local_file.app_secret.content_md5
    manifests_hash      = sha1(join("", [for f in fileset("${var.k8s_manifests_path}/app", "*.yaml") : filesha1("${var.k8s_manifests_path}/app/${f}")]))
    metrics_server_hash = filesha1("${var.k8s_manifests_path}/metrics-server/components.yaml")
  }

  provisioner "local-exec" {
    command = "aws eks update-kubeconfig --name ${aws_eks_cluster.oficina.name} --region ${var.aws_region} && kubectl apply -f ${var.k8s_manifests_path}/namespace.yaml && kubectl apply -f ${var.k8s_manifests_path}/metrics-server/ && kubectl apply -f ${path.module}/rendered/app-configmap.yaml && kubectl apply -f ${path.module}/rendered/app-secret.yaml && kubectl apply -f ${var.k8s_manifests_path}/mailhog/ && kubectl apply -f ${var.k8s_manifests_path}/app/deployment.yaml -f ${var.k8s_manifests_path}/app/service.yaml -f ${var.k8s_manifests_path}/app/hpa.yaml"
  }
}
