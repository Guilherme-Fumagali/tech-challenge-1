# GitHub Actions — Base de Conhecimento

> Síntese das 5 aulas do módulo "Software Architecture" (Fase 2 PosTech FIAP): 01-Introdução, 02-Fundamentos básicos, 03-GitHub Actions em Ambientes On-Premises, 04-Conceitos básicos de Docker, 05-Repositório de imagens Docker. Objetivo: referência rápida para decisões de CI/CD do `oficina-api` (ver [[00-requisitos-fase2]]). Não é transcrição literal — é condensação para consulta.

## Contexto

GitHub Actions é a plataforma de automação nativa do GitHub (desde 2018) para criar pipelines de CI/CD dentro do próprio repositório, sem ferramenta externa. CI/CD é a base conceitual:

- **Integração Contínua (CI)**: devs integram código frequentemente num repositório compartilhado; a cada integração, build + testes automatizados rodam para detectar erros cedo. Antes disso era comum branches longos e merges dolorosos — CI resolve isso com verificação constante.
- **Entrega/Distribuição Contínua (CD)**: evolução da CI — todo código que passa no pipeline fica automaticamente pronto (ou é implantado) em ambiente de teste/produção, com testes adicionais (UI, carga, integração, API) além dos testes de unidade.

## Conceitos fundamentais

Hierarquia de um pipeline no GitHub Actions:

- **Workflow**: processo automatizado definido em arquivo `.yml`/`.yaml` dentro de `.github/workflows/` no repositório. Cada arquivo = um pipeline. Composto por um ou mais **jobs** e acionado por **eventos**.
- **Trigger / Evento (`on`)**: atividade que dispara o workflow. Principais: `push`, `pull_request`, `schedule` (cron), entre outros. Pode-se combinar múltiplos eventos e restringir por branch.
- **Job**: unidade de execução do workflow — uma fase do pipeline (ex.: build, test, deploy). Cada job roda numa instância de **runner** própria e isolada, com seu próprio ambiente. Jobs podem rodar em paralelo (padrão) ou em sequência (via `needs`), e cada um pode usar um runner diferente (ex.: um em `ubuntu-latest`, outro em `windows-latest`).
- **Step**: comando individual dentro de um job, executado sequencialmente na mesma máquina/runner. Um step executa um comando shell (`run`) ou uma **action** (`uses`).
- **Action**: menor bloco reutilizável — encapsula uma sequência de comandos/rotina (ex.: `actions/checkout@v2`). Pode ser oficial do GitHub, da comunidade (Marketplace) ou customizada (própria, em qualquer linguagem executável no runner).
- **Runner**: agente/máquina que efetivamente executa os jobs. Pode ser hospedado pelo GitHub (`ubuntu-latest`, `windows-latest`, etc.) ou **self-hosted** (ver seção dedicada abaixo).
- **Secrets**: não é aprofundado em detalhe nas aulas, mas é citado como boa prática — nunca deixar credenciais expostas; usar segredos do GitHub (`secrets.<NOME>`) referenciados via `${{ secrets.X }}` nos workflows (uso já presente no `ci.yml` deste repo, ex. `SONAR_TOKEN`).
- **Matrix (matriz de jobs)**: citada como estratégia de otimização de recursos — permite testar em múltiplos ambientes/versões em paralelo a partir de um único job template (limite: 256 jobs por execução de workflow).

### Sintaxe YAML básica (conforme aulas)

Workflow mínimo:

```yaml
name: Meu primeiro fluxo de trabalho
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v2
```

Trigger combinando eventos e branches:

```yaml
on:
  push:
    branches: [master]
  pull_request:
    branches: [master]
```

Múltiplos jobs com runners diferentes:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v2

  test:
    runs-on: windows-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v2
      - name: Run tests
        run: dotnet test
```

Steps com ação + comandos shell:

```yaml
steps:
  - name: Checkout code
    uses: actions/checkout@v2
  - name: Install dependencies
    run: npm install
  - name: Run tests
    run: npm test
```

Pipeline CI/CD completo com dependência entre jobs (`needs`) — exemplo de build → test → deploy condicionado ao sucesso do build:

```yaml
on:
  push:
    branches:
      - main
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v2
      - name: Build
        run: make
      - name: Test
        run: make test
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        run: make deploy
```

### Boas práticas citadas nas aulas

- **Segurança**: cuidado com actions de terceiros (rodam com os mesmos privilégios do usuário/repo) — usar apenas fontes confiáveis e **fixar versão** (evitar `@latest`, preferir tag/SHA fixo).
- **Otimização de recursos**: minutos de execução são limitados/pagos — usar matriz de jobs para paralelizar testes e cache de dependências para acelerar instalação.
- **Documentação**: manter workflows comentados e descritos para que o time entenda o que cada pipeline faz.

## Runners self-hosted / On-Premises

On-Premises = infraestrutura de TI mantida fisicamente dentro da organização (vs. nuvem hospedada remotamente).

**Vantagens On-Premises**: controle total sobre sistemas/dados, políticas de segurança próprias, atende requisitos de conformidade que exigem armazenamento local.
**Desvantagens**: custo (hardware, manutenção, pessoal), escalabilidade mais lenta (requer planejamento/compra), responsabilidade total por redundância/disaster recovery.

**Runners** são os agentes que hospedam a execução dos jobs. Podem ser hospedados pelo GitHub ou **self-hosted**. Motivos para usar self-hosted:

- Hardware/software específico não disponível nos runners do GitHub.
- Restrições de rede — job precisa acessar recursos internos, ou políticas de segurança proíbem execução na nuvem pública.
- Controle/gerenciamento total da infraestrutura de execução (patches, atualizações, monitoramento sob suas próprias regras).
- Economia de custo se já há capacidade computacional ociosa própria.
- Configuração personalizada (dependências e ferramentas específicas).
- Desempenho — cargas pesadas podem rodar mais rápido localmente.

Contrapartida: responsabilidade total por manutenção/segurança dos runners e pelo custo da infraestrutura.

**Boas práticas para self-hosted runners**:
- Isolar workloads de produção dos de dev/teste.
- Manter runner, SO e dependências sempre atualizados.
- Restringir acesso ao runner (quem pode enviar jobs, o que o runner pode fazer); não guardar segredos no runner.
- Monitoramento e logging para auditoria/detecção rápida de problemas.
- Plano de disaster recovery / redundância (inclusive plano de migrar para runners hospedados pelo GitHub em emergência).
- Rotatividade regular / runners efêmeros para reduzir janela de exposição a ameaças persistentes.
- Gerenciamento de capacidade (escalar runners conforme demanda do time).

**Limites de uso (GitHub Actions, sujeitos a mudança)**:
- Execução de workflow: máx. 35 dias (inclui espera/aprovação).
- Job em fila em runner self-hosted: máx. 24h antes de falhar.
- Requisições à API do GitHub: até 1.000/hora por repositório.
- Matriz de jobs: máx. 256 jobs por execução de workflow.
- Fila de execuções de workflow: máx. 500 a cada 10s por repositório.
- Runners self-hosted registrados: máx. 10.000 por grupo.

> Para a Fase 2, o caminho mais simples/recomendado é usar runners hospedados pelo GitHub (`ubuntu-latest`) — self-hosted só se justifica se o cluster K8s for estritamente local/interno sem acesso via internet.

## Integração com Docker

### Conceitos de Docker (Aula 04)

- **Container**: pacote leve e independente contendo tudo que a aplicação precisa (código, libs, variáveis de ambiente). Isola processos mas compartilha o kernel do SO com o host — mais leve que uma VM completa.
- **Imagem**: pacote estático, autônomo e executável — o "modelo" a partir do qual containers são criados. Composta por camadas empilhadas (cada uma = uma instrução do Dockerfile), permitindo reuso/cache entre imagens.
- **Container** = instância em execução (viva) de uma imagem.
- Resolve o clássico problema "funciona na minha máquina" ao garantir paridade entre dev/teste/produção.
- Casos de uso citados: ambientes de dev/teste consistentes, pipelines CI/CD (automatizar build/test/deploy), arquiteturas de microsserviços, orquestração (via Kubernetes).

### Repositórios de imagens Docker (Aula 05)

Um **repositório de imagens** é o armazenamento centralizado onde imagens são guardadas, recuperadas, compartilhadas e gerenciadas — análogo a uma "biblioteca" (ou o "GitHub das imagens Docker", no caso do Docker Hub). Pode ser **público** (aberto, gratuito) ou **privado** (acesso restrito).

Comandos básicos (Docker Hub):

```bash
# Baixar imagem (tag padrão = latest se omitida)
docker pull nginx:latest

# Identificar imagem local
docker images

# Renomear/taguear para o formato do registry
docker tag minha_imagem meu_usuario/nome_do_repositorio:tag

# Autenticar
docker login

# Enviar (publicar) a imagem
docker push meu_usuario/nome_do_repositorio:tag
```

**Principais serviços de registry** (comparativo das aulas):

| Registry | Descrição | Destaques |
|---|---|---|
| Docker Hub | Registry padrão/mais popular | Público e privado, scan de segurança, integração CI/CD |
| GHCR (GitHub Container Registry) | Não citado nominalmente na aula, mas é a opção natural de integração com GitHub Actions (mesmo ecossistema/autenticação via `GITHUB_TOKEN`) | — |
| Google Container Registry (GCR/Artifact Registry) | Google Cloud | Integração nativa GCP, IAM, repositórios privados |
| Amazon ECR | AWS | Integração com ECS/Kubernetes, replicação entre regiões |
| Azure Container Registry (ACR) | Microsoft Azure | Integração com Azure DevOps, autenticação avançada |
| GitLab Container Registry | Integrado ao GitLab | CI/CD nativo do GitLab |
| Quay (Red Hat) | Foco enterprise | Scan automatizado, controle de acesso granular |
| JFrog Artifactory | Repositório universal (multi-formato) | Suporta Docker + outros pacotes, integra com várias ferramentas CI/CD |

**Considerações de segurança para registries** (aplicável ao escolher/operar o registry da Fase 2):
- Varredura de vulnerabilidades regular nas imagens armazenadas.
- Assinatura/verificação de imagens (integridade e autenticidade).
- Controle de acesso rigoroso (quem pode pull/push).
- Políticas de segurança específicas conforme necessidade (ex.: MFA, restrição de rede).
- Logging e monitoramento de atividades no repositório.
- Atualização/patch constante das imagens base.
- Compliance/auditorias quando aplicável (GDPR, HIPAA, PCI-DSS).

> Para o `oficina-api`, GHCR (`ghcr.io`) é a opção mais direta: mesma autenticação do GitHub Actions (`GITHUB_TOKEN`), sem necessidade de conta/segredo extra em outro provedor — mas Docker Hub também é uma alternativa válida, exigindo `DOCKERHUB_USERNAME`/`DOCKERHUB_TOKEN` como secrets do repositório.

## Aplicação no `oficina-api`

Esqueleto de pipeline combinando os conceitos das aulas com os requisitos obrigatórios da Fase 2 (build Maven → testes → build/push da imagem Docker → deploy do banco → apply dos manifestos K8s). **É um ponto de partida prático, não conteúdo literal das aulas** — ajustar nomes de secrets, cluster/kubeconfig, estratégia de migrations e namespace conforme a infraestrutura real provisionada em [[terraform]].

Já existe um workflow de CI (`/.github/workflows/ci.yml`) fazendo build+test+SonarCloud (`mvn -B verify sonar:sonar`). O esqueleto abaixo é pensado como um **segundo workflow** (`ci-cd.yml`) ou uma extensão do mesmo, adicionando os estágios de imagem e deploy que a Fase 2 exige.

```yaml
# .github/workflows/ci-cd.yml
# Esqueleto de pipeline CI/CD para a Fase 2 — ajustar registry, secrets e
# nomes de recursos K8s antes de usar em produção. Assume:
#   - Manifestos Kubernetes em /k8s (Deployments, Services, ConfigMaps/Secrets, HPA)
#   - kubeconfig do cluster salvo como secret (ex.: gerado pelo Terraform em /infra)
#   - Registry: GHCR (ghcr.io) — trocar por Docker Hub/ECR/ACR se necessário

name: CI/CD Pipeline

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  IMAGE_NAME: ghcr.io/${{ github.repository }}/oficina-api

jobs:
  # 1) Build da aplicação + testes automatizados (Maven)
  build-and-test:
    name: Build & Test
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build and run tests
        run: mvn -B verify
        # "verify" já roda os testes unitários/integração (Testcontainers) do módulo

  # 2) Build da imagem Docker + push para o registry
  build-and-push-image:
    name: Build & Push Docker Image
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'  # só publica imagem a partir da main
    permissions:
      contents: read
      packages: write  # necessário para push no GHCR
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.IMAGE_NAME }}:${{ github.sha }}
            ${{ env.IMAGE_NAME }}:latest

  # 3) Deploy do banco de dados no cluster
  #    (proposta prática: Postgres como StatefulSet/Deployment gerenciado via manifesto
  #    próprio em /k8s, ou provisionado fora do cluster via Terraform — ajustar conforme
  #    decisão de arquitetura em [[terraform]] e [[kubernetes]])
  deploy-database:
    name: Deploy Database
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up kubectl
        uses: azure/setup-kubectl@v4

      - name: Configure kubeconfig
        run: |
          mkdir -p "$HOME/.kube"
          echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > "$HOME/.kube/config"

      - name: Apply database manifests
        run: kubectl apply -f k8s/database/
        # ex.: k8s/database/{configmap.yaml,secret.yaml,statefulset.yaml,service.yaml}

  # 4) Deploy da aplicação no cluster Kubernetes (apply dos manifestos em /k8s)
  deploy-app:
    name: Deploy to Kubernetes
    needs: [build-and-push-image, deploy-database]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up kubectl
        uses: azure/setup-kubectl@v4

      - name: Configure kubeconfig
        run: |
          mkdir -p "$HOME/.kube"
          echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > "$HOME/.kube/config"

      - name: Apply application manifests
        run: kubectl apply -f k8s/app/
        # ex.: k8s/app/{deployment.yaml,service.yaml,configmap.yaml,secret.yaml,hpa.yaml}

      - name: Set new image on deployment
        run: |
          kubectl set image deployment/oficina-api \
            oficina-api=${{ env.IMAGE_NAME }}:${{ github.sha }} \
            --namespace=oficina

      - name: Wait for rollout
        run: kubectl rollout status deployment/oficina-api --namespace=oficina
```

Pontos a decidir/ajustar ao implementar de fato (fora do escopo do que as aulas cobrem, decisão de arquitetura do time):

- Secret `KUBE_CONFIG`: kubeconfig do cluster (gerado pelo Terraform em `/infra`) codificado em base64, salvo em GitHub Secrets.
- Se o cluster for local/on-premises sem exposição à internet, considerar runner **self-hosted** dentro da rede do cluster (ver seção acima) em vez do runner hospedado pelo GitHub.
- Estratégia de migrations do banco (Flyway/Liquibase, se adotado) deve rodar como um `Job` Kubernetes (`kubectl apply -f k8s/database/migration-job.yaml`) ou como init container, antes do deploy da aplicação — não coberto pelas aulas, é decisão de implementação.
- Trocar `docker/login-action` + `ghcr.io` por Docker Hub/ECR/ACR conforme registry escolhido (ver tabela comparativa acima).
- Fixar versões das actions de terceiros (`@v4`, `@v5` etc., ou SHA) — boa prática de segurança citada na Aula 02.

## Ver também

- [[dockerizacao]] — Dockerfile, docker-compose e estratégia de imagem do `oficina-api`.
- [[kubernetes]] — manifestos em `/k8s` (Deployments, Services, ConfigMaps/Secrets, HPA).
- [[terraform]] — provisionamento do cluster e do banco em `/infra`.
- [[devops]] — visão geral de DevOps/CI-CD além do GitHub Actions.
