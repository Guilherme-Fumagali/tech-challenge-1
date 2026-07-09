# Dockerização

> Fonte: `artefatos-tech-challenge-2/aulas/dockerizacao/Aula 01..06.pdf` (disciplina Software Architecture, Fase 2, PosTech FIAP). Síntese condensada para consulta durante o desenvolvimento — não é transcrição literal.
>
> Aulas cobertas: 01-Introdução ao Docker, 02-Gerenciamento de Contêineres, 03-Orquestração de Contêineres, 04-Melhores Práticas e Solução de Problemas, 05-Segurança de Contêineres, 06-ECS.

## Contexto

Requisito obrigatório da Fase 2 (ver [[00-requisitos-fase2]]): revisar/atualizar `Dockerfile` e `docker-compose.yml` do `oficina-api` (Spring Boot 3.3 / Java 21 / Maven / PostgreSQL) aplicando boas práticas de conteinerização. Este documento consolida a teoria das 6 aulas e termina com um checklist aplicado ao estado atual do repositório.

---

## Conceitos fundamentais

### Container x Máquina Virtual
- Docker (2013, dotCloud, Open Source) usa recursos nativos do **Kernel Linux** — isolamento de processos e virtualização — sem precisar de um SO completo por instância, ao contrário de VMs.
- Duas tecnologias do kernel viabilizam o isolamento:
  - **Cgroups**: limitam/priorizam uso de recursos (CPU, memória) por processo; permitem restringir, pausar ou reiniciar processos de um container sem afetar os demais.
  - **Namespaces**: isolam propriedades globais (rede, usuários, disco, PID) por container, de forma lógica e transparente. Garantem que a falha de um container (ex.: 100% de CPU) não afete os demais containers rodando no mesmo host (analogia do navio cargueiro: um container cai no mar, os outros seguem intactos).

### Imagens
- Empacotam SO base, dependências, portas expostas e variáveis de ambiente necessárias para rodar a aplicação.
- Construídas a partir de instruções declaradas em um **Dockerfile**.
- Cada instrução do Dockerfile gera uma **camada (layer)**; a instrução `FROM` é a camada zero. Camadas são cumulativas e hierárquicas — a de baixo depende da de cima.

### Cache de camadas
- Ao rebuildar uma imagem, o Docker reaproveita (cache) todas as camadas anteriores à primeira que sofreu alteração; a partir da camada alterada, tudo é reconstruído do zero.
- Impacto prático medido em aula: build "frio" ~55s x build com cache ~15s.
- Implicação de design de Dockerfile: **posicionar instruções que mudam pouco (instalação de dependências) antes das que mudam com frequência (código-fonte)** para maximizar acerto de cache. Ex.: copiar `pom.xml`/`package.json` e instalar dependências antes de copiar o restante do código.
- Cache de camadas também vale para registries remotos (Docker Hub, AWS ECR, Azure Container Registry, Google Cloud Container Registry).

### Docker Hub / Registries
- Docker Hub funciona como um "GitHub de imagens": repositórios públicos/privados, imagens oficiais e não oficiais (comunidade), possibilidade de `pull`/`push` via URL + tag.
- **Tags** versionam a imagem (correções de bug, melhorias, patches de segurança). `latest` é a última versão, mas variações de base (ex.: `alpine`, `slim`) aparecem como tags separadas.
- Cuidado ao escolher imagens de terceiros na busca do Docker Hub: nem toda imagem retornada é oficial/verificada — risco de imagens suspeitas/maliciosas.
- Alternativas de registry em nuvem citadas: **AWS ECR**, Azure Container Registry, Google Cloud Container Registry.

### Dockerfile — instruções centrais
- `FROM`: imagem base (camada zero).
- `COPY`: copia arquivos do host para a imagem (gera camada; sensível a cache).
- `RUN`: executa comando durante o build (instalação de pacotes, build da aplicação).
- `EXPOSE`, `ENTRYPOINT`/`CMD`: porta exposta e comando de start do container.
- `ARG` x `ENV`: `ARG` define valores recebidos em tempo de **build** (`--build-arg`), evitando expor segredos hardcoded no arquivo; pode alimentar uma variável `ENV` (persistida na imagem/runtime). Boa prática citada explicitamente na Aula 04 para não deixar valores sensíveis fixos no Dockerfile.
- **Multi-stage build**: usa múltiplos `FROM` no mesmo Dockerfile — um estágio compila/builda (ex.: com JDK/Maven), outro estágio final copia somente o artefato resultante para uma imagem base enxuta (ex.: JRE). Reduz drasticamente o tamanho final e a superfície de vulnerabilidades, pois ferramentas de build não vão para a imagem de produção.

### Docker Compose
- Orquestra múltiplos containers a partir de um arquivo declarativo `docker-compose.yml`.
- Estrutura principal:
  - `services`: cada serviço é um container/imagem.
    - `image` ou `build` (aponta um Dockerfile; `context` define o diretório de build).
    - `ports`: `portaHost:portaContainer` — porta do container é a usada para comunicação interna entre serviços; porta do host é a exposta externamente.
    - `depends_on`: ordena inicialização (ex.: API só sobe depois do banco saudável).
    - `environment` / `env_file`: variáveis de ambiente (o segundo aponta para um `.env`).
    - `volumes`: montagem de disco persistente.
    - `command` / `entrypoint`: sobrescreve o `CMD`/`ENTRYPOINT` da imagem.
    - `restart`: política de reinício — `no`, `on-failure` (reinicia se saída ≠ 0), `always` (sempre reinicia), `unless-stopped` (reinicia exceto se parado manualmente).
  - `network`: cria rede dedicada para comunicação entre serviços.
  - `volume`: define discos nomeados reutilizáveis por múltiplos containers.
- Comandos principais: `docker compose up`, `build`, `logs`, `restart`, `ps`, `scale`, `start`, `stop`, `down` (para e remove containers, redes, volumes associados).

### Redes e volumes
- Redes Docker permitem comunicação entre containers por nome de serviço; a integração com o host se dá via mapeamento de portas.
- Volumes garantem persistência de dados fora do ciclo de vida do container.
- **Backup de volumes** é recomendado para recuperação em caso de falha/exclusão acidental.
- **Containers de dados**: padrão onde um container nomeia um volume que é consumido por outros containers, evitando gerenciar diretórios persistentes manualmente (pasta gerenciada automaticamente pelo Docker).

---

## Gerenciamento e orquestração de contêineres

### Comandos de gerenciamento (Aula 02)
- `docker container ls -a`: lista containers (rodando ou não), com colunas relevantes: `CONTAINER ID`, `IMAGE`, `COMMAND`, `CREATED`, `STATUS`, `PORTS`, `NAMES`.
- Limitar recursos na criação: `docker run --cpus=1 -m 512m ...`.
- Inspecionar configuração aplicada: `docker container inspect <id> | grep -i cpu|mem`.
- Ajustar recursos de um container **em execução**, sem recriá-lo: `docker container update`.
- Logs e métricas de uso são inspecionáveis por container.

### Orquestração com Docker Compose (Aula 03)
- Permite subir múltiplos serviços relacionados (ex.: app + banco + api gateway) com um único comando, a partir de instruções declarativas.
- Suporta apontar `build` para um Dockerfile local (não apenas `image` pronta), com `context` definindo onde o Dockerfile está.
- Instrução `links` (legada) conecta explicitamente um container a outro.
- Padrão de uso comum: banco de dados + serviço de migrations + API gateway (exemplo de aula: Postgres + migrations + Kong/Konga).

### AWS ECS como orquestrador gerenciado (Aula 06)
Ver seção dedicada [AWS ECS](#aws-ecs) abaixo — é a camada de orquestração em nuvem coberta no curso, alternativa/complemento ao Kubernetes (ver [[kubernetes]]) para rodar containers em escala.

---

## Boas práticas de Dockerfile

Direto da Aula 04 (Melhores Práticas e Solução de Problemas) e reforços das Aulas 02/05:

1. **Multi-stage build**: separar estágio de build (com JDK/toolchain completo) do estágio final (runtime enxuto), copiando apenas o artefato compilado entre estágios.
2. **Ordenar instruções por frequência de mudança**: dependências/manifestos primeiro (cache reaproveitável), código-fonte por último — minimiza rebuilds completos.
3. **Usar `ARG` para valores sensíveis/variáveis de build**, evitando hardcode de segredos no Dockerfile; combinar com `ENV` quando o valor precisa persistir em runtime. Passar via `--build-arg` no `docker build`.
4. **Preferir imagens base menores/enxutas** (ex.: variantes `alpine`, `slim`): menos pacotes pré-instalados = menos superfície de vulnerabilidade e imagens menores (aula demonstra `node:18` x `node:18-alpine` com redução drástica de CVEs).
5. **Entender e explorar o cache de camadas** — inclusive em registries remotos — para acelerar builds locais e em CI/CD.
6. Consultar as camadas de imagens de terceiros no Docker Hub antes de adotá-las (aba "Tags"/histórico de layers) para avaliar tamanho e composição.

Boas práticas adicionais amplamente aceitas no ecossistema Docker, consistentes com os princípios acima e aplicáveis ao contexto Java/Maven (extrapoladas dos princípios de "imagem enxuta" e "menor superfície" ensinados, para aplicação prática — ver checklist abaixo):
- `.dockerignore` para não copiar artefatos desnecessários (`target/`, `.git/`, `.idea/`) para o contexto de build.
- `HEALTHCHECK` na imagem para permitir orquestradores (Compose, ECS, Kubernetes) monitorarem a saúde do container.
- Fixar tags de imagem base por versão (evitar `latest` em produção) para builds reprodutíveis.

---

## Segurança de contêineres

Aula 05 foca em análise de vulnerabilidades com a ferramenta open source **Trivy**:

- Instalação (WSL/Ubuntu): download do binário via `wget` do release do GitHub (`aquasecurity/trivy`), mover para `/usr/local/bin/`, validar com `trivy version`.
- Build da imagem a ser analisada com tag: `docker build -t teste-trivy:latest .`
- Scan: `trivy image teste-trivy:latest`.
- Filtrar por severidade: `trivy image --severity CRITICAL,HIGH <imagem>`. Níveis possíveis: `Unknown`, `Low`, `Medium`, `High`, `Critical`.
- O relatório do Trivy linka cada CVE a uma página de detalhe (Aqua Vulnerability Database) explicando a vulnerabilidade e o pacote/versão afetados.
- **Mitigação central demonstrada**: trocar a imagem base por uma variante mais enxuta (ex.: `node:18` → `node:18-alpine`) reduz drasticamente (no exemplo, a zero) as vulnerabilidades reportadas, pois há menos pacotes/bibliotecas do SO instalados.
- Objetivo prático: rodar scan de vulnerabilidades **antes de publicar/promover uma imagem**, corrigindo ou aceitando riscos conscientemente por severidade.

Boas práticas de segurança de containers reforçadas pelo conjunto das aulas (imagens enxutas, cuidado na escolha de imagens de terceiros no Docker Hub — Aula 02, uso de `ARG` para não commitar segredos — Aula 04):
- Preferir imagens oficiais/verificadas e minimalistas (`alpine`, `distroless` quando aplicável).
- Nunca commitar segredos (senhas, chaves JWT, tokens) em `Dockerfile`, `docker-compose.yml` versionado ou imagens — usar variáveis de ambiente injetadas externamente, secrets managers ou `.env` fora do controle de versão.
- Escanear imagens (Trivy ou equivalente) como parte do pipeline de CI/CD (ver [[github-actions]]), falhando o build acima de um limiar de severidade.
- Rodar o processo da aplicação como **usuário não-root** dentro do container (mitigação padrão de segurança de containers, alinhada ao objetivo da aula de "não deixar a aplicação exposta").

---

## AWS ECS

Aula 06 — ECS (Elastic Container Service): serviço gerenciado da AWS para executar, escalar e gerenciar containers Docker em cluster.

### Conceitos fundamentais
- **Cluster**: conjunto lógico de instâncias EC2 ou tarefas Fargate onde os containers rodam.
- **Task (tarefa)**: menor unidade de execução; definida por uma *task definition* (imagem Docker, CPU, memória, portas, variáveis de ambiente).
- **Service**: mantém uma quantidade desejada de tasks rodando simultaneamente, reinicia em caso de falha e pode distribuir carga entre instâncias.

### Modelos de execução
- **EC2**: mais controle sobre as instâncias subjacentes (você gerencia os servidores).
- **Fargate**: serverless — elimina a necessidade de gerenciar servidores, simplificando a operação.

### Integrações e benefícios
- **IAM**: controle de acesso e fluxo de credenciais (instância → agente ECS → task, via "segredo do contêiner").
- **CloudWatch**: monitoramento.
- **ELB (Elastic Load Balancing)**: balanceamento de carga.
- **VPC**: isolamento de rede dos recursos.
- Auto-scaling baseado em métricas de CPU/memória.

### Casos de uso citados
Ambientes de dev/test descartáveis, arquiteturas de microsserviços (um container por serviço), aplicações de alta disponibilidade/desempenho, processamento em lote/Big Data.

> Nota: o curso trata ECS como o orquestrador gerenciado da AWS; o requisito obrigatório da Fase 2 do Tech Challenge usa **Kubernetes** (ver [[kubernetes]]) como orquestrador-alvo, não ECS — os conceitos (cluster, task/pod, service, scaling, load balancer) são análogos entre as duas tecnologias.

---

## Aplicação no `oficina-api`

Estado atual do repositório (`Dockerfile` e `docker-compose.yml` na raiz) avaliado à luz das boas práticas acima:

### Dockerfile — pontos já corretos
- ✅ Já usa **multi-stage build** (`builder` com `eclipse-temurin:21-jdk-alpine` + estágio final com `eclipse-temurin:21-jre-alpine`).
- ✅ Imagem final já é baseada em **JRE** (não JDK) e em variante **alpine** (menor, menos CVEs — alinhado à Aula 05).
- ✅ `COPY pom.xml .` antes de `COPY src ./src` já favorece cache de camadas nas dependências (embora o projeto não use Maven wrapper com `.m2` cacheado — ver abaixo).

### Dockerfile — gaps a corrigir (checklist)
- [ ] **Usuário não-root**: o container roda como root por padrão. Adicionar `RUN addgroup -S spring && adduser -S spring -G spring` e `USER spring` antes do `ENTRYPOINT`, no estágio final.
- [ ] **`.dockerignore` ausente** na raiz do projeto (confirmado: não existe). Criar incluindo ao menos `target/`, `.git/`, `.idea/`, `*.md`, `docs/`, `.github/` — reduz contexto de build e evita copiar artefatos irrelevantes/sensíveis.
- [ ] **Instalação de Maven via `apk add` a cada build**: `RUN apk add --no-cache maven && mvn package ...` reinstala o Maven completo em toda execução do estágio builder. Alternativas: usar imagem base `maven:3.9-eclipse-temurin-21-alpine` no estágio builder (Maven já incluso) ou adicionar o **Maven Wrapper** (`mvnw`) ao repositório para builds reprodutíveis sem depender do Maven do host/imagem.
- [ ] **Cache de dependências Maven não aproveitado entre builds**: hoje `mvn package` baixa todas as dependências a cada build (a camada de `COPY pom.xml` ajuda no cache de camada Docker, mas não persiste o `.m2` entre execuções distintas de `docker build` sem BuildKit cache mount). Considerar `RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -q` (requer BuildKit) para acelerar builds locais/CI.
- [ ] **`HEALTHCHECK` ausente no Dockerfile da API** (o `docker-compose.yml` já tem healthcheck para o `db`, mas não para o serviço `api`). Adicionar instrução `HEALTHCHECK` apontando para um endpoint Actuator (`/actuator/health`, se habilitado) ou usar o `healthcheck:` no compose.
- [ ] **Tag de imagem base fixa por versão específica** (ex.: `eclipse-temurin:21.x.y-jre-alpine`) em vez de tag "móvel" `21-jre-alpine`, para builds reprodutíveis — avaliar trade-off com atualizações de patch de segurança automáticas.
- [ ] **Scan de vulnerabilidades**: rodar `trivy image` na imagem final como parte do fluxo de desenvolvimento/CI (ver [[github-actions]]) antes de publicar.

### docker-compose.yml — gaps a corrigir (checklist)
- [ ] **Segredos hardcoded no compose versionado**: `POSTGRES_PASSWORD: oficina`, `DB_PASS: oficina` e principalmente `JWT_SECRET: minha-chave-jwt-segura-para-producao-minimo-32-chars` estão em texto puro no arquivo. Mover para um `.env` (fora do controle de versão, com `.env.example` versionado) consumido via `env_file`, e nunca reutilizar o mesmo `JWT_SECRET` de exemplo em produção.
- [x] `ADMIN_PASSWORD: ${ADMIN_PASSWORD:-admin123}` já demonstra o padrão correto (variável de ambiente com default apenas para dev) — replicar esse padrão para `DB_PASS`/`JWT_SECRET`.
- [ ] **`depends_on` com `condition: service_healthy`** já está correto para o `db` — mas o serviço `api` não expõe seu próprio `healthcheck`, o que impede outros serviços (ex.: futura fila, gateway) de aguardar a API ficar saudável.
- [ ] **`version: "3.9"`** no topo do compose está deprecated no Compose Specification atual (Aula 03 cita a spec como referência viva) — pode ser removido sem impacto funcional nas versões recentes do Docker Compose.
- [ ] Avaliar se `ports: "5432:5432"` do banco precisa ficar exposto ao host em todos os ambientes, ou se pode ser removido em cenários onde só a `api` (na mesma rede Compose) precisa acessar o Postgres — reduz superfície exposta localmente.
- [ ] Confirmar existência de rede nomeada explícita (`networks:`) se o projeto crescer para mais serviços (cache, mensageria) — hoje a rede default implícita do Compose é suficiente para os 2 serviços atuais.

### Alinhamento com o restante da Fase 2
- A imagem construída aqui é a mesma publicada/consumida pelos manifestos Kubernetes (ver [[kubernetes]]) e pela pipeline de CI/CD (ver [[github-actions]]): o `HEALTHCHECK`/endpoint de saúde da API deve ser reaproveitado como `livenessProbe`/`readinessProbe` no K8s.
- O scan de vulnerabilidade (Trivy) e o build multi-stage devem ser etapas do pipeline de CI antes do `docker push` para o registry usado pelo cluster.
