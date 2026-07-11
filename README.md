# Oficina Mecânica API

Sistema backend MVP para gestão de uma oficina mecânica de médio porte.
Desenvolvido como Tech Challenge da Pós-Graduação em Arquitetura de Software (PosTech FIAP) —
**Fase 1** (MVP/Clean Architecture) e **Fase 2** (evolução + infraestrutura completa, abaixo).

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=alert_status&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=coverage&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=bugs&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=security_rating&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=sqale_rating&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)

---

## Fase 2 — Evolução e Infraestrutura

A Fase 2 evoluiu o MVP da Fase 1 (regras de negócio novas) e entregou toda a cadeia de
infraestrutura: containerização revisada, Kubernetes, Terraform (dois cenários) e CI/CD completo.

### O que mudou na aplicação

- **Listagem de OS ordenada por prioridade de negócio** (`Em Execução > Aguardando Aprovação >
  Em Diagnóstico > Recebida`, mais antigas primeiro dentro do mesmo status) e **exclusão lógica**
  de OS Finalizada/Entregue da listagem (nunca exclusão física).
- **Aprovação/reprovação externa via token por OS** — `POST /api/ordens/{id}/aprovar-externo`,
  endpoint público protegido por um token único (uso único, com expiração), simulando um link
  recebido por e-mail — sem precisar de login.
- **Notificação por e-mail real** (`SmtpNotificacaoService`, via MailHog em dev/demo) — o cliente
  recebe o link/token de aprovação por e-mail assim que o orçamento é gerado.
- Débitos técnicos da Fase 1 quitados: MapStruct órfão removido, `fromPersistencia` reduzido a 1
  parâmetro (`DadosOrdemServico`), Domain Storytelling referenciado no README, CVEs reverificadas.

### Arquitetura de infraestrutura

```
                        ┌──────────────┐        ┌─────────────────┐
  GitHub Actions ──CI──▶│ build + test │──CD───▶│  GHCR (imagem)  │
                        └──────────────┘        └────────┬────────┘
                                                           │
                     ┌─────────────────────────────────────┼─────────────────────┐
                     │  Kubernetes (kind local ou AWS EKS)   ▼                     │
                     │   ┌───────────────┐   ┌────────────────────┐               │
                     │   │ oficina-api   │──▶│ Postgres (StatefulSet│ local)     │
                     │   │ (2-8 réplicas,│   │ ou RDS gerenciado    │ aws)       │
                     │   │ HPA CPU/mem)  │   └────────────────────┘               │
                     │   └───────┬───────┘                                        │
                     │           ▼                                                │
                     │      MailHog (SMTP demo)                                   │
                     └─────────────────────────────────────────────────────────────┘
```

### Como rodar

| Cenário | Como | Guia |
|---|---|---|
| Dev rápido (inner-loop) | `docker compose up --build` | acima, seção "Execução local" |
| Paridade com Kubernetes (local) | `cd infra/environments/local && terraform apply` | [`infra/README.md`](infra/README.md) |
| Kubernetes "cru" (manifests) | `kubectl apply -f k8s/...` | [`k8s/README.md`](k8s/README.md) |
| AWS (EKS + RDS) — **custo real** | `cd infra/environments/aws && terraform apply` | [`infra/README.md`](infra/README.md) — ⚠️ ler o aviso de custo antes |

### CI/CD

- `.github/workflows/ci.yml` — build, testes, JaCoCo, OWASP dependency-check, SonarCloud (inalterado da Fase 1).
- `.github/workflows/terraform.yml` — `plan`/`apply` automáticos pro ambiente `local` (kind, grátis,
  smoke test em toda mudança em `infra/**`/`k8s/**`); `plan`/`apply` pro ambiente `aws` com **aprovação
  manual obrigatória** antes do `apply` (GitHub Environment `aws-production`).
- `.github/workflows/cd.yml` — build + push da imagem pro GHCR e deploy no EKS a cada push em `main`.
- `.github/workflows/bootstrap-aws.yml` — cria/remove o backend de state (S3 + DynamoDB) com um clique.
- `.github/workflows/destroy-aws.yml` — desliga o ambiente AWS com um clique (mesmo gate de aprovação).

Depois de um único passo local (`infra/bootstrap/github-oidc.sh`, que dá ao GitHub Actions acesso
via OIDC à conta AWS — impossível automatizar, já que criar a primeira credencial exigiria já ter
uma), **todo o ciclo de vida da infraestrutura roda pelo Actions**: bootstrap → plan → apply →
deploy → destroy. Ver [`infra/README.md`](infra/README.md).

### Links

- Collection Postman/Swagger: `http://localhost:8080/swagger-ui.html` (local) — link público a definir.
- Vídeo de demo (≤15min — deploy, CI/CD, consumo da API, auto-scaling): _a definir_.

---

## Objetivo

Substituir processos manuais (planilhas e anotações) por um sistema integrado que permita:

- Acompanhar em tempo real o status dos serviços
- Autorizar reparos adicionais via API
- Controlar estoque de peças e insumos
- Gerar relatórios de tempo médio de execução

---

## Arquitetura

O sistema adota **Clean Architecture** (Arquitetura em Camadas), com separação estrita entre:

```
src/main/java/com/oficina/mecanica/
├── domain/          # Entidades, Value Objects, Exceções, Interfaces de Repositório
│                    # Puro Java — zero dependência de framework
├── application/     # Use Cases — orquestra o domínio
└── infrastructure/  # Spring Boot, JPA, REST, Security, Flyway
```

### Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.4 |
| Build | Maven |
| Banco de dados | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Auth | Spring Security + JWT (JJWT 0.12) |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Container | Docker + Docker Compose |

### Por que PostgreSQL?

1. **Domínio relacional**: Cliente → Veículos → OS → Itens — relacionamentos naturais com FK e JOINs
2. **ACID obrigatório**: decremento atômico de estoque ao adicionar Peça à OS — sem race condition
3. **Precisão financeira**: `NUMERIC(10,2)` para valores de orçamento sem arredondamento de ponto flutuante
4. **Relatório de tempo médio**: `AVG(data_conclusao - data_inicio)` em uma única query SQL
5. **Integração nativa** com Spring Data JPA + Hibernate + Flyway

### Serviço de Notificação e o ACL do Context Map

> **Atualização (Fase 2)**: o canal concreto discutido abaixo como Hot Spot em aberto na Fase 1
> foi implementado — `SmtpNotificacaoService` envia e-mail real via SMTP (MailHog em dev/demo),
> incluindo o token de aprovação externa (`POST /api/ordens/{id}/aprovar-externo`). O stub de log
> continua disponível (`NOTIFICACAO_CANAL=log`, default) — a escolha do canal é uma property, não
> uma decisão de código.

#### Por que o MVP da Fase 1 não implementou um canal concreto

No Event Storming, o **Serviço de Notificação** (envio do orçamento ao cliente via email/SMS) foi modelado como um **Sistema Externo (SE)** e marcado como **Hot Spot** — dependência fora da fronteira do sistema com decisões de negócio ainda abertas.

A decisão de não implementar um canal concreto no MVP foi intencional por três razões:

1. **Escopo do MVP**: O requisito é o *back-end de gestão*, não a camada de comunicação com o cliente. O critério de aceite "envio do orçamento para aprovação" está satisfeito pelo fluxo: `POST /gerar-orcamento` muda o status para `AGUARDANDO_APROVACAO` e o cliente consulta via `GET /api/ordens/{id}/status` (endpoint público, sem autenticação).

2. **Canal indefinido**: O Event Storming levantou explicitamente a dúvida *"como o cliente é notificado — email, SMS ou push?"*. Implementar um canal específico sem essa decisão seria uma suposição arquitetural embutida em código — dívida técnica desde o dia zero.

3. **Fronteira de bounded context**: No Context Map, o Serviço de Notificação é um sistema externo com relacionamento **ACL** (Anti-Corruption Layer). O ACL foi implementado — o canal concreto é que ficou em aberto (até a Fase 2).

#### O que é o ACL e onde ele está no código

Um **Anti-Corruption Layer** é uma camada de tradução que impede que o modelo de domínio de um sistema externo "vaze" para dentro do seu bounded context. Sem ele, tipos e conceitos do fornecedor (ex.: `SendGridMessage`, `TwilioResponse`) contaminam as regras de negócio.

No Context Map deste projeto:

```
[Bounded Context: Oficina] ──ACL──> [Sistema Externo: Notificação]
```

O ACL está implementado como uma **porta de saída** (output port) na camada de Application, com **dois adaptadores** na Infrastructure alternáveis por property (`app.notificacao.canal`):

```
application/port/NotificacaoService.java                  ← fronteira do ACL (linguagem do domínio)
infrastructure/notification/LogNotificacaoService.java     ← adaptador stub (canal=log, default)
infrastructure/notification/SmtpNotificacaoService.java    ← adaptador real (canal=smtp)
```

O domínio fala apenas a linguagem do negócio:

```java
// Porta de saída — Application layer — zero dependência de framework ou provedor
public interface NotificacaoService {
    void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal, String tokenAprovacao);
}
```

O adaptador real (Infrastructure) traduz para o sistema externo e absorve toda a complexidade do protocolo — resolve o e-mail do cliente, monta a mensagem com o token de aprovação externa, e envia via `JavaMailSender`:

```java
@Component
@ConditionalOnProperty(prefix = "app.notificacao", name = "canal", havingValue = "smtp")
public class SmtpNotificacaoService implements NotificacaoService {

    @Override
    public void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal, String tokenAprovacao) {
        var cliente = clienteRepository.buscarPorId(clienteId).orElseThrow(...);
        // monta SimpleMailMessage com instruções de POST /api/ordens/{id}/aprovar-externo
        mailSender.send(mensagem);
    }
}
```

O stub (`LogNotificacaoService`) implementa a mesma interface e só registra via SLF4J — o domínio não percebe a diferença entre os dois:

```java
@Component
@ConditionalOnProperty(prefix = "app.notificacao", name = "canal", havingValue = "log", matchIfMissing = true)
public class LogNotificacaoService implements NotificacaoService {
    @Override
    public void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal, String tokenAprovacao) {
        log.info("[NOTIFICAÇÃO] OS={} | Cliente={} | Total=R$ {} | Token={}", osId, clienteId, valorTotal, tokenAprovacao);
    }
}
```

#### Fluxo de chamada

```
GerarOrcamentoUseCase
  │
  ├─ os.gerarOrcamento(validadeToken)   ← regra de negócio (domínio puro) — gera token de aprovação externa
  ├─ repository.salvar(os)              ← porta de persistência
  └─ notificacaoService                 ← porta de notificação (ACL boundary)
       .notificarOrcamentoPendente(os.getId(), os.getClienteId(), os.calcularOrcamento(), os.getTokenAprovacaoExterna())
            │
            └─ Log ou Smtp NotificacaoService (via app.notificacao.canal)
                 (canal=smtp: e-mail real via MailHog; canal=log: apenas registra)
```

#### Arquivos envolvidos

| Arquivo | Camada | Papel no ACL |
|---|---|---|
| `application/port/NotificacaoService.java` | Application | Fronteira do ACL — linguagem do domínio |
| `infrastructure/notification/LogNotificacaoService.java` | Infrastructure | Adaptador stub (canal=log) |
| `infrastructure/notification/SmtpNotificacaoService.java` | Infrastructure | Adaptador real (canal=smtp) |
| `application/usecase/ordemservico/GerarOrcamentoUseCase.java` | Application | Consumidor da porta |

Para integrar outro canal (SendGrid, SES, Twilio), basta criar uma nova classe que implemente `NotificacaoService`, anotá-la com `@ConditionalOnProperty` pro valor desejado de `app.notificacao.canal` — nenhuma regra de negócio é alterada.

---

## Pré-requisitos

- Docker e Docker Compose instalados
- Portas `8080`, `5432`, `1025` e `8025` disponíveis (API, Postgres, SMTP e UI do MailHog)

---

## Execução local (modo recomendado)

```bash
# Clone o repositório
git clone <URL_DO_REPOSITORIO>
cd oficina-api

# Copie o .env de exemplo e ajuste se quiser (os defaults já funcionam)
cp .env.example .env

# Suba banco, MailHog e a API com Docker Compose
docker compose up --build
```

A API estará disponível em: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`
MailHog (e-mails capturados, incluindo o token de aprovação externa): `http://localhost:8025`

Para rodar em paridade com Kubernetes (não apenas Docker Compose), ver `infra/environments/local`
(cluster `kind` provisionado via Terraform) e `k8s/README.md`.

---

## Execução sem Docker (desenvolvimento)

**Pré-requisito**: PostgreSQL rodando localmente na porta 5432.

```bash
# Criar banco
psql -U postgres -c "CREATE DATABASE oficina;"
psql -U postgres -c "CREATE USER oficina WITH PASSWORD 'oficina';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE oficina TO oficina;"

# Build e execução
export JAVA_HOME=<caminho-do-seu-jdk-21>
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--DB_URL=jdbc:postgresql://localhost:5432/oficina --DB_USER=oficina --DB_PASS=oficina --JWT_SECRET=minha-chave-super-secreta-32chars"
```

---

## Variáveis de ambiente

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/oficina` | URL do banco |
| `DB_USER` | `oficina` | Usuário do banco |
| `DB_PASS` | `oficina` | Senha do banco |
| `JWT_SECRET` | `minha-chave-...` | Chave HMAC — algoritmo auto-selecionado pelo JJWT pelo tamanho (≥32 chars→HS256, ≥48→HS384, ≥64→HS512) |
| `JWT_ACCESS_EXPIRATION_MS` | `900000` (15 min) | Expiração do access token em ms |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 dias) | Expiração do refresh token em ms |
| `PORT` | `8080` | Porta da aplicação |

---

## Autenticação

Todas as rotas administrativas exigem JWT. A consulta de status da OS é pública.

```bash
# 1. Obter token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Resposta: {"accessToken":"eyJ...","refreshToken":"uuid-opaco","tipo":"Bearer","expiresIn":900}

# 2. Usar o token nas requisições
curl http://localhost:8080/api/clientes \
  -H "Authorization: Bearer eyJ..."
```

---

## Fluxo principal — Ordem de Serviço

```
POST /api/ordens                        → Abre OS (status: Recebida)
POST /api/ordens/{id}/iniciar-diagnostico → Status: Em Diagnóstico
POST /api/ordens/{id}/servicos          → Adiciona serviço (snapshot de preço)
POST /api/ordens/{id}/pecas             → Adiciona peça (decrementa estoque)
POST /api/ordens/{id}/gerar-orcamento   → Status: Aguardando Aprovação (gera token + envia e-mail)
POST /api/ordens/{id}/aprovar           → Status: Em Execução (interno, via JWT)
POST /api/ordens/{id}/concluir          → Status: Finalizada (exclusão lógica da listagem)
POST /api/ordens/{id}/entregar          → Status: Entregue

# Fluxo alternativo (reprovação):
POST /api/ordens/{id}/reprovar          → Status: Cancelada + estorno de estoque

# Aprovação externa (sem JWT — token único por OS, enviado por e-mail):
POST /api/ordens/{id}/aprovar-externo   → {"token": "...", "decisao": "APROVAR"|"REPROVAR"}

# Consulta pública (sem JWT):
GET  /api/ordens/{id}/status

# Listagem (ordenada por prioridade de status, exclui Finalizada/Entregue):
GET  /api/ordens
```

---

## Endpoints disponíveis

| Grupo | Base URL | Autenticação |
|---|---|---|
| Auth | `POST /api/auth/login` · `POST /api/auth/refresh` · `POST /api/auth/logout` | Pública |
| Clientes | `GET/POST/PUT/DELETE /api/clientes` | JWT |
| Veículos | `GET/POST/PUT/DELETE /api/veiculos` | JWT |
| Serviços | `GET/POST/PUT/DELETE /api/servicos` | JWT |
| Peças/Estoque | `GET/POST/PUT/DELETE /api/pecas` | JWT |
| Ordens de Serviço | `GET/POST /api/ordens` + ações de ciclo de vida | JWT |
| Status OS (público) | `GET /api/ordens/{id}/status` | **Pública** |
| Aprovação externa (público) | `POST /api/ordens/{id}/aprovar-externo` | **Pública** (token por OS) |
| Relatório | `GET /api/ordens/relatorio/tempo-medio` | JWT |

Documentação completa: `http://localhost:8080/swagger-ui.html`

---

## Testes

```bash
# Rodar todos os testes (requer Docker para Testcontainers)
./mvnw test

# Relatório de cobertura (gerado em target/site/jacoco/index.html)
./mvnw verify
```

Os testes cobrem:

- **Domain**: `OrdemServico`, `Peca`, `CpfCnpj`, `Placa` — sem Spring, execução em milissegundos
- **Application**: `AdicionarPecaAOSUseCase`, `ReprovarOrcamentoUseCase` — com mocks Mockito
- **Integração**: fluxo completo de OS e estorno de estoque com Testcontainers (PostgreSQL real)

---

## Documentação DDD

Localizada em [`docs/ddd/`](docs/ddd/):

| Artefato | Arquivo |
|---|---|
| Event Storming | `docs/ddd/event_storming.drawio` (`.pdf`) |
| Context Map | `docs/ddd/context_map.drawio` (`.pdf`) |
| Domain Storytelling | `docs/ddd/domain_storytelling.drawio` (`.svg`) |
| Linguagem Ubíqua | `docs/ddd/linguagem_ubiqua.md` |

---

## Relatório de vulnerabilidades

Gerado via OWASP Dependency Check:

```bash
./mvnw dependency-check:check
# Relatório: target/dependency-check-report.html
```

---

## Débitos técnicos conhecidos

| # | Débito | Status |
|---|--------|--------|
| 1 | ~~Mappers manuais nos adapters de persistência~~ | **Resolvido (Fase 2)** — a dependência MapStruct estava declarada no `pom.xml` mas nunca chegou a ser usada (zero `@Mapper` no código); removida em vez de adotada, para não carregar um processador de anotação sem uso real nesse tamanho de base de código |
| 2 | ~~Serviço de notificação stub~~ | **Resolvido (Fase 2)** — `SmtpNotificacaoService` envia e-mail real via SMTP (MailHog em dev/demo), ativado por `NOTIFICACAO_CANAL=smtp`. `LogNotificacaoService` continua disponível como fallback (`NOTIFICACAO_CANAL=log`, default) |
| 3 | ~~`fromPersistencia` com 9 parâmetros (Sonar S107)~~ | **Resolvido (Fase 2)** — extraído para o record `DadosOrdemServico` (`domain/entity/DadosOrdemServico.java`); `OrdemServico.reconstituir(DadosOrdemServico)` agora recebe 1 parâmetro |
| 4 | **CVEs sem patch disponível** — ver detalhamento abaixo | Reverificado na Fase 2 — ver resultado abaixo |

### Reverificação de CVEs (débito #4)

Procedimento: atualizar `spring-boot-starter-parent` para o último patch disponível e rodar `mvn org.owasp:dependency-check-maven:check`, conferindo `target/dependency-check-report.html`.

- `spring-boot-starter-parent` atualizado de `3.4.5` → `3.4.7` nesta fase (traz patches de Tomcat/Spring Security mais recentes).
- **Reverificação concluída em 09/07/2026**: `mvn org.owasp:dependency-check-maven:check` rodou
  limpo (build não quebrou o gate `failBuildOnCVSS=9`) — as mesmas 4 CVEs (`CVE-2026-22732`,
  `CVE-2025-55754`, `CVE-2025-66614`, `CVE-2026-29145`) continuam sem patch publicado pelos
  fornecedores (Spring Security e Apache Tomcat) mesmo após o upgrade de patch do Spring Boot.
  Supressões mantidas em `owasp-suppressions.xml` com a data atualizada; reavaliar quando os
  mantenedores publicarem correção.

---

## Estrutura do repositório

```
oficina-api/
├── src/
│   ├── main/
│   │   ├── java/com/oficina/mecanica/
│   │   │   ├── domain/          # Puro Java
│   │   │   ├── application/     # Use Cases
│   │   │   └── infrastructure/  # Spring, JPA, REST, Security
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/    # Flyway SQL
│   └── test/
├── docs/
│   ├── ddd/                     # Event Storming, Context Map, Domain Storytelling
│   └── base-conhecimento/       # Requisitos e notas técnicas da Fase 2
├── k8s/                         # Manifests Kubernetes (app, database, mailhog)
├── infra/
│   ├── bootstrap/                # Scripts de pré-requisito AWS (OIDC + backend de state)
│   └── environments/
│       ├── local/                 # Terraform — cluster kind
│       └── aws/                    # Terraform — EKS + RDS
├── .github/workflows/            # ci.yml, terraform.yml, cd.yml, bootstrap-aws.yml, destroy-aws.yml
├── observability/                # Config do stretch de OpenTelemetry (Tempo + Grafana)
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── pom.xml
```
