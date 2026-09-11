# ADR-001 — Quatro repositórios, com `oficina-api` preservado

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

O enunciado da Fase 3 exige organizar o projeto em **quatro repositórios separados**, cada um com CI/CD e deploy automático: Lambda, infraestrutura Kubernetes, infraestrutura do banco e aplicação principal.

Hoje tudo vive num monorepo (`oficina-api`) que carrega o histórico das Fases 1 e 2: código da aplicação, `/k8s`, `/infra` e `.github/workflows`. Esse histórico tem valor de avaliação — mostra a evolução do projeto ao longo das três fases — e valor prático, porque a configuração de OIDC, secrets e backend de state já está funcionando ali.

Restrição: o usuário `soat-architecture` precisa ser adicionado a todos os repositórios, e cada um precisa de `README.md` próprio com diagrama de arquitetura específico.

## Decisão

`oficina-api` **permanece** como o repositório da aplicação (repo 4), com todo o histórico. Criamos três repositórios novos:

| Repositório | Conteúdo | Origem |
|---|---|---|
| `oficina-auth-lambda` | Function de autenticação por CPF + Lambda authorizer, projeto SAM | novo |
| `oficina-infra-k8s` | Terraform de rede, EKS, ECR, API Gateway, New Relic; manifests `k8s/` | movido de `oficina-api` |
| `oficina-infra-db` | Terraform de RDS, subnet group, security group | movido de `oficina-api` |
| `oficina-api` | Aplicação Spring Boot | permanece |

`/infra` e `/k8s` saem de `oficina-api` por **remoção simples** — o conteúdo é copiado para os repos novos e removido do original em um commit que referencia os destinos.

A **rede (VPC, subnets, NAT, route tables) fica em `oficina-infra-k8s`**, não num quinto repositório, porque o enunciado fixa quatro. O repo de banco consome os IDs de rede via SSM Parameter Store.

## Alternativas consideradas

**Extrair com `git filter-repo`, preservando o histórico dos arquivos movidos.** Mais fiel à história do projeto. Descartada porque a reescrita de histórico é uma etapa de risco cujo benefício aqui é baixo: o histórico relevante de `/infra` e `/k8s` é curto (nasceram na Fase 2) e o commit de remoção em `oficina-api` já deixa o rastro de onde foram parar.

**Quatro repositórios novos do zero.** Simples de explicar, mas descarta o histórico das Fases 1 e 2 e obriga a recadastrar OIDC, secrets e proteção de branch em quatro lugares em vez de três.

**Monorepo com quatro pipelines por path filter.** Descartada: contraria o texto literal do enunciado, que diz "quatro repositórios separados".

**Quinto repositório só para rede.** Descartada pela mesma razão — o enunciado fixa quatro.

## Consequências

**Positivas**
- Histórico da aplicação intacto, incluindo os artefatos das fases anteriores em `docs/`.
- OIDC, backend de state S3+DynamoDB e secrets já configurados em `oficina-api` servem de modelo pronto para os outros três.
- Ciclos de vida independentes: mudar um alerta do New Relic não dispara build da aplicação.

**Negativas**
- **Perde-se o `terraform apply` atômico.** Hoje um apply levanta EKS e RDS juntos; passa a haver ordem obrigatória entre repos (ver [SPEC-05](../../../../artefatos-tech-challenge-3/plano-implementacao/specs/SPEC-05-infra-terraform.md) §5).
- Quatro pipelines para manter, quatro conjuntos de secrets, quatro `README.md`.
- O histórico de `/infra` e `/k8s` fica órfão: acessível em `oficina-api` até o commit de remoção, ausente nos repos novos.
- Mudança que atravessa fronteiras (ex.: nova variável de ambiente que vem do Terraform) exige dois PRs coordenados.
