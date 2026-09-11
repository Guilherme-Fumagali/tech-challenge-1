# ADR-008 — Registry de imagens migra de GHCR para ECR

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

O pipeline atual publica em `ghcr.io/guilherme-fumagali/oficina-api` e o `k8s/app/deployment.yaml` referencia essa imagem.

Com os nós indo para subnet privada ([ADR-007](./ADR-007-rede-privada-nat-instance.md)), todo pull passa a atravessar a NAT instance `t4g.nano`, que é burstable e de banda limitada. Uma imagem de aplicação Java com JRE tem centenas de MB; puxá-la simultaneamente em dois ou três nós durante um scale-out do HPA é exatamente o cenário que satura essa instância.

O role dos nós do EKS **já tem `AmazonEC2ContainerRegistryReadOnly` anexado** (`aws_iam_role_policy_attachment.eks_node_ecr_readonly`), herança da Fase 2 — a permissão existe e nunca foi usada.

A Aula 03 de Serverless ensina exatamente o fluxo de publicação no ECR: `create-repository`, `get-login-password | docker login`, `docker tag`, `docker push`.

## Decisão

Migrar o registry para **Amazon ECR**, repositório `oficina-api` em `us-east-1`, com **gateway endpoint de S3** na VPC.

O pipeline passa a autenticar por OIDC (sem `docker login` com senha) e a tag continua sendo o `github.sha`, com `latest` em paralelo.

Lifecycle policy: manter as 10 imagens mais recentes, expirar o resto.

## Alternativas consideradas

**Manter GHCR.** Zero trabalho de migração e o registry continua junto do código. Descartada porque `ghcr.io` é destino público: **todo pull passaria pela NAT instance**, competindo com a telemetria pela banda da `t4g.nano`. Com ECR + gateway endpoint de S3, as camadas vêm pelo backbone da AWS sem tocar a NAT.

**GHCR com um pull-through cache no ECR.** Resolve a banda mantendo o GHCR como origem. Descartada por adicionar uma peça a mais para configurar e depurar, com ganho nulo sobre publicar direto no ECR.

**Docker Hub.** Mesma objeção do GHCR, mais rate limit de pull anônimo.

## Consequências

**Positivas**
- Pull de imagem sai pelo gateway endpoint de S3, que é **gratuito** e não passa pela NAT.
- Autenticação por IAM/OIDC, sem token de registry para rotacionar.
- Cobre o conteúdo da Aula 03 de Serverless na prática.
- A permissão nos nós já existia — a mudança é de configuração, não de IAM.

**Negativas**
- Custo de armazenamento, ainda que baixo — US$ 0,10/GB/mês, na casa de centavos com a lifecycle policy.
- A imagem deixa de ser visível junto ao código no GitHub; quem quiser inspecioná-la precisa de credencial AWS.
- Ambiente local (`kind`, `docker compose`) precisa de `aws ecr get-login-password` para puxar a imagem publicada, ou continuar buildando localmente — o que já é o caso.
- Um passo a mais na pipeline: login no ECR antes do push.
