# ADR-008 — Registry de imagens migra de GHCR para ECR

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

O pipeline atual publica em `ghcr.io/guilherme-fumagali/oficina-api`, e o `k8s/app/deployment.yaml` referencia essa imagem.

Com os nós migrando para subnet privada ([ADR-007](./ADR-007-rede-privada-nat-instance.md)), todo pull passa a atravessar a NAT instance `t4g.nano`, que é burstable e tem banda limitada. Uma imagem de aplicação Java com JRE tem centenas de MB; puxá-la simultaneamente em dois ou três nós durante um scale-out do HPA é o cenário que satura essa instância.

O role dos nós do EKS já tem `AmazonEC2ContainerRegistryReadOnly` anexado (`aws_iam_role_policy_attachment.eks_node_ecr_readonly`), herança da Fase 2; a permissão existe, mas nunca foi usada.

A Aula 03 de Serverless apresenta o fluxo de publicação no ECR: `create-repository`, `get-login-password | docker login`, `docker tag`, `docker push`.

## Decisão

Migrar o registry para Amazon ECR, repositório `oficina-api` em `us-east-1`, com gateway endpoint de S3 na VPC.

O pipeline passa a autenticar por OIDC (sem `docker login` com senha), e a tag continua sendo o `github.sha`, com `latest` em paralelo.

Lifecycle policy: manter as 10 imagens mais recentes e expirar as demais.

## Alternativas consideradas

**Manter GHCR.** Não exige trabalho de migração e mantém o registry junto do código. Descartada porque `ghcr.io` é destino público: todo pull passaria pela NAT instance, competindo com a telemetria pela banda da `t4g.nano`. Com ECR + gateway endpoint de S3, as camadas trafegam pelo backbone da AWS sem passar pela NAT.

**GHCR com um pull-through cache no ECR.** Resolve a limitação de banda mantendo o GHCR como origem. Descartada por adicionar mais um componente para configurar e depurar, sem ganho em relação a publicar diretamente no ECR.

**Docker Hub.** Mesma objeção do GHCR, além do rate limit de pull anônimo.

## Consequências

**Positivas**
- O pull de imagem usa o gateway endpoint de S3, que é gratuito, sem passar pela NAT.
- Autenticação por IAM/OIDC, sem token de registry para rotacionar.
- Aplica na prática o conteúdo da Aula 03 de Serverless.
- A permissão nos nós já existia; a mudança é de configuração, sem alteração de IAM.

**Negativas**
- Custo de armazenamento, ainda que baixo: US$ 0,10/GB/mês, na casa de centavos com a lifecycle policy.
- A imagem deixa de ser visível junto ao código no GitHub; inspecioná-la requer credencial AWS.
- O ambiente local (`kind`, `docker compose`) precisa de `aws ecr get-login-password` para puxar a imagem publicada, ou pode continuar com build local, como já ocorre.
- Um passo adicional na pipeline: login no ECR antes do push.
