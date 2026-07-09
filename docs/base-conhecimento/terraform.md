# Terraform — Infraestrutura como Código

> Fonte: `artefatos-tech-challenge-2/aulas/terraform/POSTECH - Aula{1,2,3}_rev2.pdf` (Software Architecture, Fase 2 PosTech FIAP). Síntese condensada para consulta durante o desenvolvimento — não é transcrição literal.

Contexto: requisito obrigatório da Fase 2 é provisionar via Terraform, em `/infra`, o cluster Kubernetes (local ou cloud) e o banco de dados, documentando recursos criados e como aplicar (ver [[00-requisitos-fase2]]). Este documento cobre os conceitos ensinados nas 3 aulas de Terraform do curso e propõe um esqueleto prático para o `oficina-api`.

## Conceitos fundamentais

### Infraestrutura como Código (IaC)
Prática de definir e gerenciar infraestrutura por código versionável, em vez de configuração manual/scripts ad hoc. Benefícios citados na Aula 1: automação (menos tarefas manuais), consistência (menos erros humanos/discrepância entre ambientes), escalabilidade e rastreamento (histórico via controle de versão). Ferramentas de IaC citadas: Terraform, Ansible, Chef, CloudFormation — Terraform se destaca por integrar muitos provedores e usar uma linguagem própria simples (HCL).

### HCL (HashiCorp Configuration Language)
Linguagem declarativa criada pela HashiCorp para Terraform, Nomad e Consul. Você declara o **estado desejado**; a ferramenta decide como chegar lá. Características: legível por humanos, extensível/estruturada (blocos, atributos, expressões, condicionais), conversível para JSON.

Elementos principais (exemplo da Aula 1, cluster EKS):

```hcl
resource "aws_eks_cluster" "eks_cluster_fiap" {
  name     = "fiap-eks"
  role_arn = var.awsAcademyRole        # variável (ver abaixo)

  vpc_config {                          # sub-bloco: configuração de rede
    subnet_ids         = ["${var.subnetA}", "${var.subnetB}", "${var.subnetC}"]
    security_group_ids = ["${var.securityGroupId}"]
  }

  access_config {                       # sub-bloco: modo de autenticação
    authentication_mode = "API_AND_CONFIG_MAP"
  }
}
```

- **Bloco**: estrutura básica, definida por tipo (`resource`) + tipo de recurso (`"aws_eks_cluster"`) + nome/identificador local (`"eks_cluster_fiap"`).
- **Atributos/argumentos**: propriedades dentro do bloco (`name`, `role_arn`).
- **Sub-blocos**: configuração adicional aninhada (`vpc_config`, `access_config`).
- **Variáveis**: declaradas com `variable "nome" { default = ... }` e referenciadas com `var.nome`, permitem reuso e abstração de valores.
- **Interpolação**: `"${...}"` para combinar valores/gerar strings dinâmicas e referenciar atributos de outro recurso (`aws_eks_cluster.eks_cluster_fiap.name`).
- **Condicionais e funções**: HCL suporta `for` e filtros (`if`) dentro de expressões, ex.: `[for subnet in data.aws_subnet.subnet : subnet.id if subnet.availability_zone != var.regionDefault]`.

> As aulas mencionam "modularização" como boa prática geral (organizar configuração em múltiplos arquivos/módulos), mas não detalham a sintaxe de `module` blocks — tratar como próximo passo de estudo, não como conteúdo já coberto.

### Providers
Plugins que traduzem HCL em chamadas de API de uma plataforma específica (AWS, Kubernetes, DNS, etc.), tornando o Terraform multicloud/agnóstico. Mantidos pela HashiCorp ou pela comunidade.

```hcl
provider "aws" {
  region = "us-east-1"
}

provider "kubectl" {}   # provider da comunidade para aplicar manifests K8s via Terraform
```

Desde o Terraform 0.13, providers referenciados no código são baixados automaticamente do Registry (oficial ou customizado) durante o `init`.

**Fixar versão dos providers** (evita quebras por atualização inesperada):

```hcl
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.45.0"
    }
    kubectl = {
      source  = "gavinbunney/kubectl"
      version = ">= 1.7.0"
    }
  }
}
```

Autenticação: nunca hardcodar credenciais no `.tf` — usar variáveis de ambiente, arquivos de config locais (ex.: `~/.aws/credentials`) ou um cofre de segredos (ex.: HashiCorp Vault).

### State (tfstate)
Arquivo JSON que é a "verdade única" da infraestrutura gerenciada pelo Terraform: identificadores dos recursos, atributos (nomes, tamanhos, IPs), dependências e metadata interna. Gerado/atualizado automaticamente por `apply`/`destroy`.

Fluxo interno em toda execução:
1. **Leitura do estado** — carrega o tfstate atual.
2. **Comparação** — estado atual (tfstate) vs. desejado (arquivos `.tf`).
3. **Criação do plano** — o que precisa ser criado/modificado/destruído.
4. **Atualização do estado** — tfstate é reescrito após aplicar mudanças.

Cuidados: fazer backup regular (perda/corrupção dificulta sincronizar a infra real com o Terraform); usar **bloqueio de estado** (lock) em back-ends que suportam (S3+DynamoDB, Terraform Cloud) para evitar que duas execuções concorrentes corrompam o state.

### Backend (local vs. remoto)
Interface que o Terraform usa para armazenar o state e executar operações relacionadas (armazenamento, lock, execução remota).

| | Local (default) | Remoto (recomendado em equipe) |
|---|---|---|
| Onde fica o state | Arquivo `terraform.tfstate` no diretório do projeto | S3, Azure Blob Storage, GCS, Terraform Cloud |
| Vantagens | Simples, ideal para projeto pessoal/pequeno | Colaboração, criptografia/backup, lock, acesso centralizado |
| Desvantagens | Sem colaboração, sem lock, risco de perda do arquivo | Requer configuração inicial e depende de serviço externo |

```hcl
terraform {
  backend "s3" {
    bucket = "fiap-backend-tf"
    key    = "eks/terraform.tfstate"
    region = "us-east-1"
  }
}
```

### Variables e Outputs
- **Variables** (`variable "x" { default = ... }` / `var.x`): parametrizam a configuração (já visto acima).
- **Outputs**: expõem valores do state para consulta externa ou encadeamento entre configurações.

```hcl
output "bucket_arn" {
  value = aws_s3_bucket.backend-tf.arn
}
```
Lido com `terraform output` (ver seção seguinte).

## Fluxo de trabalho Terraform

Comandos cobertos na Aula 3, na ordem típica de uso:

| Comando | Função | Quando usar |
|---|---|---|
| `terraform init` | Baixa providers, configura o backend, valida versões/dependências. Sempre o primeiro comando num diretório novo. | Novo projeto; após alterar provider ou backend no `.tf`. |
| `terraform validate` | Checa sintaxe HCL e uso correto dos blocos, sem tocar infraestrutura. | Após criar/editar `.tf`, antes de `plan`. |
| `terraform plan` | Compara tfstate vs. `.tf` e mostra o que seria criado/alterado/destruído, sem aplicar. | Sempre antes de `apply`; para revisar impacto em equipe. |
| `terraform apply` | Executa o plano: cria/atualiza/destrói recursos e atualiza o tfstate. Pede confirmação salvo `-auto-approve`. | Após revisar o `plan`. |
| `terraform show` | Exibe o estado atual detalhado (recursos e atributos) a partir do tfstate. | Inspecionar infra atual; depuração. |
| `terraform destroy` | Remove todos os recursos rastreados no tfstate, de forma ordenada. Pede confirmação salvo `-auto-approve`. | Desmontar ambiente de teste; reset antes de reconfigurar. |
| `terraform output` | Lê o tfstate e imprime os valores declarados em blocos `output`. | Compartilhar dados (ex.: endpoint, ARN) com outra equipe/sistema. |
| `terraform refresh` | Sincroniza o tfstate com o estado real da infraestrutura (detecta mudanças feitas fora do Terraform). | Antes de novo `plan`, se algo mudou manualmente. |
| `terraform state` (`mv`, `rm`, `list`) | Manipula o arquivo de estado diretamente sem alterar a infra real. | Corrigir state corrompido; mover recursos entre módulos. |
| `terraform fmt` | Formata o código `.tf` (indentação/espaçamento) segundo o padrão HCL. | Após escrever/editar arquivos, por consistência. |
| `terraform graph` | Gera grafo de dependências (formato DOT, visualizável com Graphviz). | Entender ordem de criação em infra grande/complexa. |
| `terraform import` | Traz um recurso já existente (criado fora do Terraform) para dentro do state gerenciado. | Migrar infraestrutura legada para IaC. |

Flags úteis vistas na Aula 3 (em `apply`/`destroy`):
- `-auto-approve`: pula a confirmação interativa.
- `-var-file=<arquivo>`: aplica um arquivo de variáveis específico (ex.: `.tfvars`) — mecanismo citado no material para parametrizar `apply`, base do uso de `.tfvars` por ambiente.
- `-target=<recurso>`: restringe a operação a um recurso específico.

Fluxo padrão: `init` → `validate` → `plan` → `apply` (revisão humana no meio) → ... → `destroy` quando o ambiente não é mais necessário. Workspaces (múltiplos states isolados por ambiente) **não foram cobertos** nestas 3 aulas — se for necessário separar dev/prod, considerar diretórios/backends distintos por ambiente até estudar workspaces formalmente.

## Boas práticas

Extraídas das Aulas 1 e 2:

1. **Fixar versões de providers** (`required_providers` com `version` exata ou faixa) — evita quebra por upgrade automático.
2. **Nunca hardcodar credenciais** no `.tf`. Usar variáveis de ambiente, arquivos de config do provedor ou um cofre (Vault).
3. **Organizar o código em múltiplos arquivos/módulos** para projetos complexos (mencionado como princípio geral; sintaxe de módulos não detalhada nas aulas 1–3).
4. **Backend remoto para state em equipe** (S3, Azure Blob, GCS, Terraform Cloud) em vez do backend local — necessário para colaboração, backup automático e lock.
5. **Habilitar lock de state** (ex.: S3 + DynamoDB) para evitar corrupção por execuções concorrentes.
6. **Sempre `plan` antes de `apply`** — revisar o diff antes de aplicar em qualquer ambiente compartilhado.
7. **`-var-file` para parametrizar por ambiente** em vez de duplicar `.tf` — separar valores (região, tamanhos, nomes) da lógica de recursos.
8. **`terraform fmt` e `terraform validate`** como parte do hábito de commit, para consistência e detecção precoce de erro de sintaxe.

## Aplicação no `oficina-api`

Esqueleto de ponto de partida para a pasta `/infra`, baseado nos conceitos das 3 aulas (providers, backend remoto, resources, variables, outputs). **É proposta prática — as aulas não ensinam RDS especificamente nem dão um exemplo completo de banco gerenciado; o bloco de RDS abaixo segue o mesmo padrão de `resource` visto em aula, adaptado ao requisito do Tech Challenge.** Ajustar ao provider real que o grupo decidir usar (AWS é o provider usado nos exemplos de aula; para ambiente local, o mesmo racional se aplica trocando o provider por `docker`/`kind` e módulos equivalentes).

### Estrutura de diretório proposta

```
infra/
├── README.md              # recursos criados + passo a passo de apply (exigência do enunciado)
├── main.tf                 # providers + módulos/recursos principais
├── variables.tf             # declaração de variables
├── outputs.tf                # outputs (endpoint do cluster, endpoint do DB, etc.)
├── backend.tf                 # configuração do backend remoto (state)
├── terraform.tfvars.example    # exemplo de valores (não versionar o .tfvars real com segredos)
└── modules/                     # (opcional, evolução futura) eks/, rds/, etc.
```

### `backend.tf` — state remoto (Aula 2)

```hcl
terraform {
  backend "s3" {
    bucket = "oficina-api-tfstate"
    key    = "fase2/terraform.tfstate"
    region = "us-east-1"
  }
}
```

### `main.tf` — provider + cluster Kubernetes (EKS, Aula 1) + banco de dados (proposta)

```hcl
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.45.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# --- Cluster Kubernetes ---
# Padrão visto na Aula 1. Em curso com AWS Academy, role_arn normalmente
# aponta para a LabRole fornecida pelo ambiente.
resource "aws_eks_cluster" "oficina_api_cluster" {
  name     = "oficina-api-eks"
  role_arn = var.eks_role_arn

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = [var.security_group_id]
  }

  access_config {
    authentication_mode = "API_AND_CONFIG_MAP"
  }
}

# --- Banco de dados PostgreSQL ---
# PROPOSTA: não coberto literalmente nas aulas de Terraform (que usam S3/EKS
# como exemplos). Segue o mesmo padrão de resource block para provisionar
# um RDS PostgreSQL gerenciado pela AWS.
resource "aws_db_instance" "oficina_api_db" {
  identifier        = "oficina-api-db"
  engine            = "postgres"
  engine_version    = "16"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  db_name           = var.db_name
  username          = var.db_username
  password          = var.db_password   # nunca hardcode: vem de variável + tfvars não versionado / secret manager
  skip_final_snapshot = true
}
```

### `variables.tf`

```hcl
variable "aws_region" {
  default = "us-east-1"
}

variable "eks_role_arn" {
  description = "ARN da role usada pelo cluster EKS (ex.: LabRole no AWS Academy)"
  type        = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "db_name" {
  default = "oficina_api"
}

variable "db_username" {
  type = string
}

variable "db_password" {
  type      = string
  sensitive = true   # evita exibir o valor em logs/plan output
}
```

### `outputs.tf`

```hcl
output "eks_cluster_endpoint" {
  value = aws_eks_cluster.oficina_api_cluster.endpoint
}

output "db_endpoint" {
  value = aws_db_instance.oficina_api_db.endpoint
}
```

### `terraform.tfvars.example`

```hcl
aws_region         = "us-east-1"
eks_role_arn       = "arn:aws:iam::xxxxxxxxxxx:role/LabRole"
subnet_ids         = ["subnet-aaa", "subnet-bbb", "subnet-ccc"]
security_group_id  = "sg-xxxxxxx"
db_username        = "oficina_admin"
# db_password NÃO deve ir neste arquivo de exemplo nem em nenhum arquivo versionado
```

### `infra/README.md` (exigência explícita do enunciado)

Conteúdo mínimo esperado (a redigir junto com o time ao finalizar o provisionamento real):
- **O que é criado**: cluster Kubernetes (EKS) + instância PostgreSQL (RDS), com suas configurações de rede (VPC/subnets/security groups).
- **Pré-requisitos**: Terraform instalado, credenciais AWS configuradas (`aws configure` ou variáveis de ambiente), bucket S3 do backend já existente (ou criado à parte antes do `init`).
- **Como aplicar**:
  ```bash
  cd infra
  cp terraform.tfvars.example terraform.tfvars   # preencher valores reais, sem commitar
  terraform init
  terraform validate
  terraform plan -var-file=terraform.tfvars
  terraform apply -var-file=terraform.tfvars
  ```
- **Como destruir** (fim do ambiente de estudo): `terraform destroy -var-file=terraform.tfvars`.
- **Outputs relevantes**: endpoint do cluster (usado pelo `kubectl`/pipeline de deploy, ver [[kubernetes]]) e endpoint do banco (usado nas variáveis de conexão da aplicação, ver [[dockerizacao]] para como isso chega ao container via env vars/secrets).

Este esqueleto é ponto de partida didático — antes de aplicar em um ambiente real, revisar: se o cluster deve ser local (kind/minikube, conforme permitido pelo enunciado) ou cloud (EKS, como nos exemplos de aula), custos do RDS, e se os security groups/subnets já existem ou também devem ser provisionados via Terraform.
