# Base de Conhecimento — Tech Challenge Fase 2

Índice de consulta sintetizado a partir do material de aula (`artefatos-tech-challenge-2/aulas/`) da Fase 2 do curso PosTech FIAP em Software Architecture. Objetivo: referência técnica rápida durante a evolução do `oficina-api`, sem precisar reabrir os PDFs originais.

| Documento | Disciplina | Conteúdo |
|---|---|---|
| [00-requisitos-fase2.md](./00-requisitos-fase2.md) | — | Requisitos obrigatórios e entregáveis da Fase 2 (fonte de verdade do escopo) |
| [clean-architecture.md](./clean-architecture.md) | Clean Architecture | Princípios, camadas, aplicação no domínio da oficina |
| [dockerizacao.md](./dockerizacao.md) | Dockerização | Docker, Dockerfile, Compose, boas práticas, segurança |
| [devops.md](./devops.md) | DevOps | CI/CD conceitos, práticas GitHub, testes em CI, qualidade de código |
| [github-actions.md](./github-actions.md) | GitHub Actions | Workflows, jobs, integração com Docker, esqueleto de pipeline |
| [kubernetes.md](./kubernetes.md) | Kubernetes (1 e 2) | Fundamentos e tópicos avançados, esqueleto de manifestos `/k8s` |
| [terraform.md](./terraform.md) | Terraform | IaC, fluxo plan/apply, esqueleto de `/infra` |
| [opentelemetry.md](./opentelemetry.md) | OpenTelemetry | Traces/metrics/logs, Jaeger/Prometheus/Loki, instrumentação |

## Como usar

- Antes de decisões de arquitetura/infra da Fase 2, consultar `00-requisitos-fase2.md` para confirmar o que é obrigatório vs. diferencial.
- Cada documento de disciplina termina com uma seção "Aplicação no `oficina-api`" — é o ponto de partida prático, não copiar sem adaptar ao estado real do código.
- Links internos usam sintaxe `[[nome-arquivo]]` (sem extensão) apontando para outro arquivo desta mesma pasta.

## Fontes

Extraído e sintetizado de: `/home/gfumagali/Documents/fiap/tech-challenge/artefatos-tech-challenge-2/aulas/` (PDFs de aula, uso exclusivo/pessoal — não redistribuir).
