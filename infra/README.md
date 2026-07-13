# Infraestrutura — Terraform

Dois ambientes, o mesmo conjunto de manifests em [`k8s/`](../k8s):

- **`local`** — cluster **kind** (Kubernetes em Docker), Postgres in-cluster, HPA. Custo zero.
  É o ambiente usado na demo.
- **`aws`** — **EKS + RDS**, provisionado do zero. Cobra dinheiro real.

O inner-loop do dia a dia continua sendo `docker compose up` na raiz do repo (Postgres +
MailHog + API, sem Kubernetes).

```
infra/
├── bootstrap/            # scripts de pré-requisito da AWS (não são Terraform — ver "Bootstrap" abaixo)
│   ├── github-oidc.sh    #   dá ao GitHub Actions acesso à conta AWS (único passo local, roda 1x)
│   └── bootstrap.sh      #   cria/remove o backend de state S3+DynamoDB (roda via Actions)
└── environments/
    ├── local/            # kind + Postgres + HPA — custo zero, roda na sua máquina
    └── aws/              # EKS + RDS — ver seção "Ambiente AWS" abaixo (custo real)
```

## Ambiente local (kind)

Sobe o cluster, constrói a imagem, carrega no nó e implanta tudo num único `apply`.

### Pré-requisitos

`docker` (Docker Desktop **ligado**), `kind`, `kubectl` e `terraform` no PATH.

### Subir

```bash
cd infra/environments/local
cp terraform.tfvars.example terraform.tfvars   # preencher as 3 senhas
terraform init
terraform apply
```

O que o apply faz, em ordem: cria o cluster kind → `docker build` da API → `kind load` da imagem
no nó → aplica namespace, metrics-server, Postgres, MailHog e a API (Deployment + Service + HPA).

### Por que build local em vez de pull do GHCR

O pacote publicado pelo CI no GHCR é **privado**. Puxá-lo de dentro do cluster exigiria um
`imagePullSecret` com um PAT do GitHub guardado no cluster — credencial de longa duração só
para uma demo. `kind load docker-image` injeta a imagem direto no nó: sem registry, sem
credencial, e funciona offline.

Para usar a imagem do CI (depois de tornar o pacote público em *Settings → Packages*):

```hcl
build_local_image = false
app_image         = "ghcr.io/<owner>/oficina-api:latest"
```

### Acessar e ver o HPA escalando

```bash
kubectl --kubeconfig=./kubeconfig get pods -n oficina -w      # aguardar Ready
kubectl --kubeconfig=./kubeconfig port-forward svc/oficina-api 8080:80 -n oficina
curl http://localhost:8080/actuator/health

kubectl --kubeconfig=./kubeconfig get hpa -n oficina -w       # em outro terminal
k6 run -e BASE_URL=http://localhost:8080 ../../../k8s/loadtest/k6-script.js
```

### Derrubar

```bash
terraform destroy
```

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

### Bootstrap: por que o backend de state não é Terraform

`environments/aws/backend.tf` guarda o state num bucket S3 com lock em DynamoDB. Esses dois
recursos **não são gerenciados por Terraform**, e isso é deliberado.

O motivo é um ovo-e-galinha: no primeiro `apply` que fosse criar o bucket, o bucket ainda não
existe — logo o state dessa execução teria que ficar em disco. Um state local morre junto com
o runner efêmero do CI (ou com a máquina de quem rodou), e na execução seguinte o Terraform
"esqueceria" que o bucket existe, tentaria recriá-lo e falharia. Contornar isso exigiria
commitar o `.tfstate` no repo ou migrar o state pra dentro do próprio bucket que ele
gerencia — ambos consertam o sintoma, não a causa.

A causa é tratar um pré-requisito de infraestrutura como recurso de aplicação. Um script
idempotente (`bootstrap/bootstrap.sh`, AWS CLI puro) **não tem state**: pode rodar N vezes,
converge sempre pro mesmo lugar, e não há nada pra perder entre execuções. Por isso ele roda
tranquilo num runner efêmero — e é por isso que existe o workflow `bootstrap-aws.yml`.

O Terraform continua sendo dono de tudo que o Tech Challenge pede (VPC, EKS, RDS); só o
backend que hospeda o próprio state dele é que fica de fora, que é a prática usual.

### Setup inicial (uma vez na vida do projeto)

**Passo 1 — dar à pipeline acesso à sua conta AWS.** Este é o **único passo que roda na sua
máquina**, e não por escolha: pra criar a credencial que o pipeline usa, o pipeline
precisaria já ter uma credencial. Alguém, uma vez, precisa rodar isso com as próprias
credenciais de admin.

```bash
aws configure                    # se ainda não tiver credenciais locais
./infra/bootstrap/github-oidc.sh # cria o OIDC provider + IAM role, imprime o ARN
```

O script cria uma IAM role que **só este repositório** consegue assumir, via OIDC — sem
access key estática guardada em secret (que vaza e nunca é rotacionada).

**Passo 2 — configurar o repositório no GitHub:**

- **Settings → Environments** → criar `aws-production` com **Required reviewers** (você
  mesmo). É esse ambiente que faz `apply`/`destroy`/`bootstrap` pausarem pedindo aprovação
  humana antes de tocar em qualquer coisa cobrada.
- **Settings → Secrets → Actions** → criar:
  `AWS_ROLE_ARN` (o ARN impresso no passo 1), `TF_VAR_DB_PASSWORD`,
  `TF_VAR_JWT_SECRET` (≥32 caracteres — HS256 exige, senão a app nem sobe),
  `TF_VAR_ADMIN_PASSWORD`.

**Daqui em diante, nada mais roda localmente.** Todo o ciclo de vida é um clique no Actions.

### Ciclo de vida (tudo via GitHub Actions)

| # | Quando | Actions → workflow | O que faz |
|---|---|---|---|
| 1 | Uma vez, antes de tudo | **Bootstrap AWS** (`action: create`) | Cria bucket S3 + DynamoDB de lock. Idempotente. |
| 2 | A cada mudança em `infra/**` | **Terraform** → `plan-aws` | Automático, só leitura, sem custo. |
| 3 | Push em `main` | **Terraform** → `apply-aws` | **Pausa aguardando aprovação.** Aplica exatamente o plano revisado. Cria VPC/EKS/RDS — **começa a cobrar aqui.** |
| 4 | A cada push em `main` | **CD** | Builda a imagem, publica no GHCR, faz rollout no EKS. Só funciona depois do passo 3. |
| 5 | **Assim que terminar a demo** | **Destroy AWS** | Destrói EKS + RDS. **Para a cobrança.** |
| 6 | No fim do projeto | **Bootstrap AWS** (`action: destroy`) | Remove o bucket + tabela. Zera a pegada na conta. |

> A ordem de 5 → 6 importa: o state vive dentro do bucket. Apagar o bucket antes de destruir
> o EKS/RDS deixaria esses recursos órfãos na conta — de pé, cobrando, e sem Terraform pra
> removê-los. O `bootstrap.sh destroy` **se recusa a rodar** se detectar que o state ainda tem
> recursos, exatamente pra impedir esse acidente.

### Sobre o nome do bucket

O bucket é criado no **account regional namespace** da AWS, não no namespace global histórico.
Daí o formato `oficina-api-tfstate-<conta>-<região>-an` — o sufixo é obrigatório nesse modo.

Dois motivos:

- **Não colide.** No namespace global, nomes são únicos entre *todas* as contas AWS do mundo:
  um `oficina-api-tfstate` qualquer pode já ter dono e o create falharia. No account regional,
  o nome é reservado à conta.
- **Não vira alvo depois de deletado.** No namespace global, ao deletar o bucket no fim do
  projeto o nome volta pro pool e outra conta pode recriá-lo — passando a receber requisições
  destinadas ao bucket antigo. No account regional isso é impossível. A AWS classifica isso
  como *security best practice*.

Isso **não afeta o Terraform**: o `--bucket-namespace` só é exigido no `CreateBucket` (que o
`bootstrap.sh` faz). Ler e escrever objetos — tudo que o backend faz — é idêntico nos dois
namespaces.

O `bootstrap.sh` **deriva** o nome da conta/região em que está rodando, então funciona em
qualquer conta sem edição. Já o `environments/aws/backend.tf` precisa do nome **literal**
(bloco `backend` não aceita variável): se trocar de conta AWS, é lá que se ajusta.

### Dry run local (opcional, sem custo)

Só leitura, pra revisar o diff antes de aprovar um `apply` de verdade:

```bash
cd infra/environments/aws
cp terraform.tfvars.example terraform.tfvars   # preencher db_password/jwt_secret reais
terraform init
terraform plan
```

### Destruir (parar a cobrança)

**Assim que terminar de gravar a demo:** Actions → **Destroy AWS** → Run workflow (mesmo gate
de aprovação). Confirme no console AWS que EKS e RDS não existem mais.

Alternativa local: `cd infra/environments/aws && terraform destroy`.
