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

> ⚠️ **Isto cobra dinheiro real assim que o `apply` roda.** Uso pontual — suba só pra gravar a
> demo/validar a entrega, e **destrua logo depois** (ver seção "Destruir" abaixo).

Conta AWS **pessoal** (não sandbox de curso), então o Terraform provisiona **tudo do zero**: VPC,
subnets públicas, Internet Gateway, IAM roles do cluster e dos nós, EKS, RDS. Não há reaproveitamento
de infraestrutura pré-existente.

### Estimativa de custo

| Recurso | Custo aproximado |
|---|---|
| EKS control plane | ~US$ 0,10/h |
| 2x EC2 `t3.small` (nós) | ~US$ 0,042/h |
| RDS `db.t4g.micro` | ~US$ 0,016/h |
| **Total** | **~US$ 0,16/h** (~US$ 3,80 se ficar 24h ligado) |

Decisões de custo mínimo: sem NAT Gateway (nós em subnets públicas com Security Group restritivo —
trade-off aceitável só porque o cluster é efêmero), instâncias pequenas, single-AZ no RDS, storage
mínimo (20GB gp3).

### Pré-requisito único: bootstrap do backend remoto

Rodar **uma única vez**, manualmente (não faz parte de nenhum pipeline):

```bash
cd infra/bootstrap
terraform init
terraform apply
```

Isso cria o bucket S3 e a tabela DynamoDB que `environments/aws/backend.tf` espera encontrar.

### Rodar localmente (dry run, sem custo)

```bash
cd infra/environments/aws
cp terraform.tfvars.example terraform.tfvars   # preencher db_password/jwt_secret reais
terraform init
terraform plan   # só leitura — revisar o diff antes de qualquer apply real
```

### CI/CD (`.github/workflows/terraform.yml` + `destroy-aws.yml`)

1. Configurar em **Settings → Environments** um ambiente chamado `aws-production` com
   **Required reviewers** (você mesmo, ou outro membro do grupo).
2. Configurar os secrets do repositório: `AWS_ROLE_ARN` (role assumível via OIDC —
   preferível — ou trocar por `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`),
   `TF_VAR_DB_PASSWORD`, `TF_VAR_JWT_SECRET`, `TF_VAR_ADMIN_PASSWORD`.
3. Qualquer mudança em `infra/**` roda `plan-aws` automaticamente. Em push pra `main`,
   `apply-aws` **pausa** aguardando aprovação no Environment antes de aplicar exatamente
   o plano já calculado.
4. Depois que `apply-aws` provisiona o cluster pela primeira vez, o `cd.yml` (deploy da
   aplicação a cada push em `main`) passa a funcionar — ele só faz `kubectl set image`
   contra um cluster que já precisa existir.

### Destruir

**Sempre que terminar de gravar a demo, destrua o ambiente AWS.** Duas formas:

- **Via GitHub Actions** (recomendado — auditável): Actions → workflow "Destroy AWS" →
  Run workflow. Mesmo gate de aprovação do `apply-aws`.
- **Localmente**: `cd infra/environments/aws && terraform destroy`.

Confirme no console AWS que o EKS e o RDS realmente não existem mais depois.
