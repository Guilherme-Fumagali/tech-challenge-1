# Oficina Mecânica API

Sistema backend MVP para gestão de uma oficina mecânica de médio porte.  
Desenvolvido como **Tech Challenge — Fase 1** da Pós-Graduação em Arquitetura de Software (PosTech FIAP).

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=alert_status&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=coverage&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=bugs&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=security_rating&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Guilherme-Fumagali_tech-challenge-1&metric=sqale_rating&token=7eca46d63f91469baf821034a54bb15eeb6341fc)](https://sonarcloud.io/summary/new_code?id=Guilherme-Fumagali_tech-challenge-1)

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
| Framework | Spring Boot 3.3 |
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

#### Por que não foi implementado um canal concreto?

No Event Storming, o **Serviço de Notificação** (envio do orçamento ao cliente via email/SMS) foi modelado como um **Sistema Externo (SE)** e marcado como **Hot Spot** — dependência fora da fronteira do sistema com decisões de negócio ainda abertas.

A decisão de não implementar um canal concreto no MVP foi intencional por três razões:

1. **Escopo do MVP**: O requisito é o *back-end de gestão*, não a camada de comunicação com o cliente. O critério de aceite "envio do orçamento para aprovação" está satisfeito pelo fluxo: `POST /gerar-orcamento` muda o status para `AGUARDANDO_APROVACAO` e o cliente consulta via `GET /api/ordens/{id}/status` (endpoint público, sem autenticação).

2. **Canal indefinido**: O Event Storming levantou explicitamente a dúvida *"como o cliente é notificado — email, SMS ou push?"*. Implementar um canal específico sem essa decisão seria uma suposição arquitetural embutida em código — dívida técnica desde o dia zero.

3. **Fronteira de bounded context**: No Context Map, o Serviço de Notificação é um sistema externo com relacionamento **ACL** (Anti-Corruption Layer). O ACL foi implementado — o canal concreto é que ficou em aberto.

#### O que é o ACL e onde ele está no código

Um **Anti-Corruption Layer** é uma camada de tradução que impede que o modelo de domínio de um sistema externo "vaze" para dentro do seu bounded context. Sem ele, tipos e conceitos do fornecedor (ex.: `SendGridMessage`, `TwilioResponse`) contaminam as regras de negócio.

No Context Map deste projeto:

```
[Bounded Context: Oficina] ──ACL──> [Sistema Externo: Notificação]
```

O ACL está implementado como uma **porta de saída** (output port) na camada de Application, combinada com um **adaptador** na Infrastructure:

```
application/port/NotificacaoService.java        ← fronteira do ACL (linguagem do domínio)
infrastructure/notification/LogNotificacaoService.java  ← adaptador stub (lado externo)
```

O domínio fala apenas a linguagem do negócio:

```java
// Porta de saída — Application layer — zero dependência de framework ou provedor
public interface NotificacaoService {
    void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal);
}
```

O adaptador (Infrastructure) traduz para o sistema externo e absorve toda a complexidade do protocolo:

```java
// Adaptador real (exemplo — não implementado no MVP)
@Component
public class SendGridNotificacaoService implements NotificacaoService {

    @Override
    public void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal) {
        // Constrói SendGridMessage (tipo do provedor — não existe no domínio)
        // Chama API do SendGrid
        // Trata erros do provedor sem expô-los ao domínio
    }
}
```

O stub atual (`LogNotificacaoService`) implementa a mesma interface e registra via SLF4J — o domínio não percebe a diferença:

```java
@Component
public class LogNotificacaoService implements NotificacaoService {
    @Override
    public void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal) {
        log.info("[NOTIFICAÇÃO] OS={} | Cliente={} | Total=R$ {}", osId, clienteId, valorTotal);
    }
}
```

#### Fluxo de chamada

```
GerarOrcamentoUseCase
  │
  ├─ os.gerarOrcamento()          ← regra de negócio (domínio puro)
  ├─ repository.salvar(os)        ← porta de persistência
  └─ notificacaoService           ← porta de notificação (ACL boundary)
       .notificarOrcamentoPendente(os.getId(), os.getClienteId(), os.calcularOrcamento())
            │
            └─ LogNotificacaoService.notificarOrcamentoPendente(...)
                 (hoje: log | amanhã: SendGrid, SES, Twilio — sem tocar no use case)
```

#### Arquivos envolvidos

| Arquivo | Camada | Papel no ACL |
|---|---|---|
| `application/port/NotificacaoService.java` | Application | Fronteira do ACL — linguagem do domínio |
| `infrastructure/notification/LogNotificacaoService.java` | Infrastructure | Adaptador stub atual |
| `application/usecase/ordemservico/GerarOrcamentoUseCase.java` | Application | Consumidor da porta |

Para integrar um canal real, basta criar uma nova classe que implemente `NotificacaoService` e anotá-la com `@Component` — nenhuma regra de negócio é alterada.

---

## Pré-requisitos

- Docker e Docker Compose instalados
- Porta `8080` e `5432` disponíveis

---

## Execução local (modo recomendado)

```bash
# Clone o repositório
git clone <URL_DO_REPOSITORIO>
cd oficina-api

# Suba o banco e a API com Docker Compose
docker-compose up --build
```

A API estará disponível em: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

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
POST /api/ordens/{id}/gerar-orcamento   → Status: Aguardando Aprovação
POST /api/ordens/{id}/aprovar           → Status: Em Execução
POST /api/ordens/{id}/concluir          → Status: Finalizada
POST /api/ordens/{id}/entregar          → Status: Entregue

# Fluxo alternativo (reprovação):
POST /api/ordens/{id}/reprovar          → Status: Cancelada + estorno de estoque

# Consulta pública (sem JWT):
GET  /api/ordens/{id}/status
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

Localizada na pasta raiz do projeto (`../`):

| Artefato | Arquivo |
|---|---|
| Event Storming | `event_storming.drawio` |
| Context Map | `context_map.drawio` |
| Linguagem Ubíqua | `linguagem_ubiqua.md` |
| Fluxos DDD (PlantUML) | `FluxosDDD_corrigidos.txt` |

---

## Relatório de vulnerabilidades

Gerado via OWASP Dependency Check:

```bash
./mvnw dependency-check:check
# Relatório: target/dependency-check-report.html
```

---

## Débitos técnicos conhecidos

| # | Débito | Impacto | Solução sugerida |
|---|--------|---------|-----------------|
| 1 | **Mappers manuais** — conversão entre entidades JPA e objetos de domínio feita manualmente nos adapters de persistência | Boilerplate repetitivo; risco de campo esquecido ao adicionar atributos | Migrar para [MapStruct](https://mapstruct.org/) (geração em compile-time, zero overhead em runtime) ou ModelMapper |
| 2 | **Serviço de notificação stub** — `NotificacaoService` é um port sem implementação concreta; e-mail/SMS não são enviados | Requisito de notificação ao cliente não atendido em produção | Implementar adapter com JavaMail, SendGrid ou AWS SES |
| 3 | **Construtor de reconstituição com muitos parâmetros** — método `fromPersistencia` em `OrdemServico` possui 9 parâmetros (Sonar S107) | Code smell de manutenibilidade | Encapsular em record `DadosOrdemServico` e receber como parâmetro único |
| 4 | **CVEs sem patch disponível** — CVE-2026-22732 (Spring Security 6.4.5) e CVE-2025-55754, CVE-2025-66614, CVE-2026-29145 (Tomcat 10.1.40) sem versão corrigida em 01/05/2026; testado também em Spring Boot 3.5.0 (Tomcat 10.1.41 + Spring Security 6.5.0) com mesmo resultado | Vulnerabilidades críticas (CVSS ≥ 9.0) suprimidas com justificativa em `owasp-suppressions.xml`; não endereçáveis por upgrade enquanto fornecedores não publicarem patch | Monitorar releases do Apache Tomcat e Spring Security; remover supressões e retestar quando patches forem publicados |

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
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```
