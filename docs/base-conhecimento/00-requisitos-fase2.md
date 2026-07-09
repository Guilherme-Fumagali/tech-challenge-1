# Tech Challenge — Fase 2 — Requisitos

> Fonte: `artefatos-tech-challenge-2/14SOAT - Fase 2 - Tech challenge.pdf`. Vale 60% da nota das disciplinas da fase.

## Problema

Oficina mecânica (sistema da Fase 1 já implantado) precisa evoluir para suportar expansão, alta disponibilidade e picos de demanda. Objetivos de negócio:

- Reduzir riscos operacionais com infraestrutura escalável.
- Automatizar provisionamento e deploy.
- Melhorar qualidade/organização do código mantendo evolução sustentável.
- Suportar grandes volumes de OS em horários de pico com escalabilidade dinâmica.

## Objetivo da fase

Evoluir a aplicação da Fase 1 garantindo qualidade, resiliência e escalabilidade, incorporando infraestrutura moderna e automação.

## Requisitos obrigatórios

### 1. Evolução da aplicação
- Refatorar código da Fase 1 aplicando:
  - Clean Code (nomes claros, simplicidade, coesão).
  - **Clean Architecture ou Arquitetura Hexagonal** (separação adequada de camadas/dependências). *(este repo já usa Clean Architecture — ver [[clean-architecture]])*
  - Testes automatizados (unit/integração) cobrindo fluxos críticos.
- Alterar/criar APIs:
  - **Abertura de OS**: recebe cliente, veículo, serviços, peças → retorna ID único da OS.
  - **Consulta de status da OS**: Recebida, Diagnóstico, Aguardando Aprovação, Execução, Finalizada, Entregue.
  - **Aprovação de orçamento**: endpoint para receber notificação externa de aprovação/recusa.
  - **Listagem de OS**:
    - Ordenar por status: `Em Execução > Aguardando Aprovação > Diagnóstico > Recebida`.
    - Dentro do mesmo status: mais antigas primeiro.
    - Excluir (lógica, não física) OS finalizadas/entregues da listagem.
  - **Atualização de status da OS** via ferramenta externa (ex.: e-mail).

### 2. Infraestrutura

| Área | Exigência | Doc relacionado |
|---|---|---|
| Conteinerização | Dockerfile atualizado + docker-compose para dev local | [[dockerizacao]] |
| Kubernetes | Manifestos: Deployments, Services, ConfigMaps/Secrets, **HPA** (CPU/memória) | [[kubernetes]] |
| IaC | Terraform provisionando cluster K8s (local ou cloud) + banco de dados; documentar recursos e como aplicar | [[terraform]] |
| CI/CD | Pipeline (GitHub Actions, GitLab CI, etc.): build app → testes → build imagem Docker → deploy cluster K8s → deploy DB → apply manifestos | [[github-actions]], [[devops]] |
| Observabilidade | Não é requisito obrigatório explícito no PDF de entrega, mas é disciplina da fase — usar para diferencial | [[opentelemetry]] |

## Entregáveis

Repositório git (mesmo da Fase 1) contendo:
- Código-fonte refatorado.
- `Dockerfile` e `docker-compose` revisados.
- Manifestos Kubernetes em **`/k8s`**.
- Scripts Terraform em **`/infra`**.
- Config da pipeline CI/CD.
- `README.md` atualizado com:
  - Descrição da solução e objetivos da fase.
  - Desenho de arquitetura: componentes da app, infra provisionada, fluxo de deploy.
  - Instruções: execução local, deploy em K8s, provisionamento Terraform.
  - Link da collection de APIs (Postman/Swagger).
  - Link de vídeo demonstrativo (YouTube/Vimeo, ≤15 min, público ou não-listado) mostrando: deploy, execução do CI/CD, consumo das APIs, escalabilidade automática.

Entrega no portal: PDF com link do repo (compartilhado com usuário **`soat-architecture`**), desenho de arquitetura, link do vídeo.

## Convenções adotadas neste repo

- `/k8s` — manifestos Kubernetes.
- `/infra` — scripts Terraform.
- `.github/workflows/` — pipelines GitHub Actions.
- `docs/base-conhecimento/` — este índice, consultar antes de decisões de arquitetura/infra da fase 2.
