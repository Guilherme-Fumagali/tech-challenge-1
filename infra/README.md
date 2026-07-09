# Infraestrutura — Terraform

Dois ambientes independentes, em pastas separadas (não workspaces) porque divergem no próprio
grafo de providers — não só em valores de variável: `local` usa `kind`+`kubernetes` sem custo,
`aws` usa `aws`+RDS e cobra por hora. Cada um tem seu próprio backend de state.

```
infra/
├── bootstrap/            # cria o backend S3+DynamoDB do ambiente aws (rodar uma única vez, manual)
└── environments/
    ├── local/             # cluster kind — grátis, efêmero, sem pré-requisitos de nuvem
    └── aws/                # EKS + RDS — ver seção "Ambiente AWS" abaixo (custo real)
```

## Ambiente local (kind)

Provisiona um cluster Kubernetes local via [kind](https://kind.sigs.k8s.io/) (Kubernetes-in-Docker)
e aplica todos os manifests de `../k8s` automaticamente.

**Pré-requisitos**: Docker rodando, `kubectl`, `kind`, `terraform` (>= 1.5) instalados.

```bash
cd infra/environments/local
terraform init
terraform plan
terraform apply
```

O `apply` cria o cluster e já aplica `namespace.yaml`, `database/`, `mailhog/` e `app/` via
`kubectl` (usando `local-exec` — o provider `kubernetes`/`kubectl` do Terraform teria o problema
de "ovo e galinha": precisaria de um cluster alcançável em tempo de `plan`, mas o cluster só
existe após este mesmo `apply`).

> Os `secret.yaml` em `../k8s` só têm placeholders (`REPLACE_ME`). Para um teste real, sobrescreva-os
> antes do `apply` — ver instruções em `../k8s/README.md`. Sem isso, os pods sobem mas a aplicação
> não autentica corretamente no Postgres/JWT.

Acesso (sem Ingress configurado, por simplicidade):
```bash
kubectl --kubeconfig=./kubeconfig port-forward svc/oficina-api 8080:80 -n oficina
```

Ao terminar:
```bash
terraform destroy
```

Este fluxo é o que o job `plan-local`/`apply-local-smoketest` do `.github/workflows/terraform.yml`
roda automaticamente a cada mudança em `infra/**` ou `k8s/**` — totalmente automatizado porque é
gratuito e descartável.

> Nota: `docker compose up` (na raiz do repo) e este fluxo via `kind` são dois caminhos locais
> **independentes e complementares** — o compose é o inner-loop rápido de desenvolvimento, o kind
> é o caminho de paridade com Kubernetes usado para validar os manifests antes de ir pra nuvem.

---

## Ambiente AWS (EKS + RDS)

Ver seção dedicada mais abaixo, adicionada junto com a Parte 6 do plano de evolução — cobre o
provisionamento de VPC/IAM/EKS/RDS, controle de custo, e como rodar/destruir com segurança.
