# OpenTelemetry

> Fonte: 5 aulas da disciplina "Software Architecture" (Fase 2 PosTech FIAP) — Aula 01 Introdução, Aula 02 Tracing (Jaeger), Aula 03 Metrics (Prometheus), Aula 04 Logs (Loki), Aula 05 Boas práticas e desafios. Hands-on das aulas usa uma app Python/Flask; síntese abaixo é conceitual e vale para qualquer stack.
>
> Observabilidade **não é requisito obrigatório explícito** da entrega da Fase 2 (ver [[00-requisitos-fase2]]), mas é disciplina da fase — vale como diferencial. Ver seção "Aplicação no oficina-api" para como plugar isso no projeto.

## Conceitos fundamentais

### Monitoramento vs. Observabilidade

- **Monitoramento**: coleta dados pré-definidos para checar saúde do sistema (dashboards/alertas sobre métricas conhecidas: CPU, taxa 5xx). Reativo, baseado em "incógnitas conhecidas" (known unknowns) — diz *que algo* está errado.
- **Observabilidade**: capacidade de entender o estado interno de um sistema complexo só observando seus outputs (telemetria), mesmo para problemas nunca previstos. Investigativo, baseado em "incógnitas desconhecidas" (unknown unknowns) — ajuda a descobrir *por que* algo está errado.
- Observabilidade não substitui monitoramento, o aprimora.

### Os três pilares

| Pilar | O que é | Quando usar | Característica |
|---|---|---|---|
| **Logs** | Registro de evento discreto em um ponto no tempo. Forma mais antiga e granular. | Depuração detalhada, análise forense, fluxo de eventos de um componente | Estruturado (JSON) >> texto livre: pesquisável, filtrável, agregável |
| **Metrics** | Agregação numérica medida ao longo do tempo. Leve e eficiente de armazenar. | Saúde geral, tendências, dashboards, alertas | Tipos: Counter, Gauge, Histogram |
| **Traces** | Jornada completa de uma requisição por múltiplos serviços = coleção de **Spans**. | Latência ponta-a-ponta, gargalos, dependências entre serviços | Span = unidade de trabalho (id, trace id, nome, timestamps, atributos) |

### Arquitetura do OpenTelemetry

- **OpenTelemetry (CNCF)** não é uma plataforma de análise — é um **padrão aberto** + conjunto de ferramentas para instrumentar, gerar, coletar e exportar telemetria. Resolve o **vendor lock-in**: instrumenta-se o código uma vez com a API OTel e troca-se de back-end via configuração, sem reescrever código.
- **API**: interfaces agnósticas de implementação que o código usa para gerar telemetria (ex.: `tracer.start_span()`).
- **SDK**: implementação concreta da API para uma linguagem (Python, Java...). Gerencia ciclo de vida dos spans, sampling e processamento.
- **Collector**: serviço/proxy que roda junto da app ou em host separado. Recebe telemetria de múltiplas fontes, processa (adiciona atributos, filtra dados sensíveis, faz sampling) e exporta para um ou mais back-ends. **Boa prática em produção**: desacopla apps dos back-ends e centraliza configuração de exportação.
- **Exporters**: traduzem dados OTel para o formato de um back-end específico (Jaeger, Prometheus, Loki, Datadog...). Formato nativo de transporte do OTel é o **OTLP** (OpenTelemetry Protocol).
- Saída no console é ótima para aprendizado/depuração local, mas impraticável em produção (impossível correlacionar milhares de linhas de JSON).

### Instrumentação automática vs. manual

- **Automática**: base recomendada — cobertura ampla com esforço mínimo, revela a estrutura principal das transações ("big picture" instantâneo). Injeção/extração de contexto para HTTP, gRPC, mensageria (RabbitMQ, Kafka) acontece automaticamente.
- **Manual**: usada para ir além do técnico e adicionar contexto de **negócio** — spans para fluxos importantes (`process-refund`, `generate-invoice`) enriquecidos com atributos de negócio (`customer.tier`, `product.category`, `is_trial_user`).
- Estratégia recomendada (Aula 05): **combinar as duas** — automática como base, manual para contexto de negócio.

### Contexto e propagação

- **Context**: objeto OTel que guarda informações da operação ativa, incluindo `TraceId` e `SpanId` do span corrente.
- **Propagação de contexto**: mecanismo que serializa esse contexto, envia pela rede (cabeçalhos de requisição) e desserializa no serviço receptor, permitindo reconstruir a árvore de chamadas entre serviços.
  1. **Injeção**: serviço cliente injeta o contexto atual nos metadados da chamada (ex.: headers HTTP).
  2. **Extração**: serviço servidor extrai o contexto dos metadados recebidos.
  3. **Criação do span filho**: com `TraceId`/`ParentSpanId` extraídos, o serviço cria seu próprio span vinculado como filho.
- Padrões de propagação:
  - **W3C Trace Context** (recomendado/padrão OTel): headers `traceparent` (versão, TraceId, ParentSpanId, flags de sampling) e `tracestate` (dados extra do fornecedor).
  - **B3 Propagation** (Zipkin, ainda comum): múltiplos headers (`X-B3-TraceId`, `X-B3-SpanId`...).
- Sem propagação de contexto: apenas spans-raiz isolados por serviço, sem conexão — anula o propósito do tracing distribuído.

## Tracing distribuído (Jaeger)

### Conceitos de tracing

- **Trace**: jornada completa de uma requisição = conjunto de spans (a "história completa de uma transação").
- **Span**: unidade de trabalho/operação (chamada HTTP, query no banco, chamada externa...). Cada serviço tocado gera pelo menos um span.
- **Relação pai-filho**: spans formam uma árvore. Primeiro span = **root span**; span criado por uma chamada subsequente = **child span** do span que o originou (**parent span**).

### Componentes de um Span (modelo de dados OTel)

1. **Nome da operação**: legível, ex. `HTTP GET /api/users`, `db.query`.
2. **Timestamps** de início e fim → duração.
3. **IDs**: `TraceId` (compartilhado por todos os spans do trace), `SpanId` (único do span), `ParentSpanId` (liga ao span pai; ausente no root span).
4. **Atributos (Attributes)**: pares chave-valor (tags) com contexto pesquisável. Seguir as **Convenções Semânticas** do OTel (`http.method`, `http.status_code`, `db.system`) para padronizar entre ferramentas.
5. **Eventos (Events)**: mensagens com timestamp dentro do span — análogo a uma linha de log anexada (ex.: "Cache miss").
6. **Status**: `Unset` (padrão), `Ok`, `Error` (opcionalmente com mensagem) — sinaliza problema para as ferramentas de visualização.
7. **Span Kind**: `SERVER` (requisição recebida), `CLIENT` (requisição feita a sistema remoto), `PRODUCER`/`CONSUMER` (mensageria), `INTERNAL` (padrão, não cruza fronteira de processo).

### Jaeger

- Projeto CNCF, criado na Uber, para armazenar/buscar/visualizar traces distribuídos. Deployável desde "all-in-one" (dev local) até arquitetura escalável de produção.
- Componentes:
  - **Jaeger Client** (obsoleto) — hoje usa-se SDK OTel + exportador OTLP.
  - **Jaeger Collector**: recebe traces (via OTLP), valida, indexa, armazena.
  - **Storage back-end**: Jaeger não tem storage próprio — integra com Elasticsearch/Cassandra (alta carga de escrita/consulta).
  - **Jaeger Query**: API para buscar/recuperar traces do storage.
  - **Jaeger UI**: interface web que consome o Jaeger Query.
- **Fluxo recomendado**: App (SDK OTel) → gera spans → exporta via OTLP → **OTel Collector** (processa: sampling, atributos) → exporta via *jaeger exporter* → Jaeger Collector → Storage (ex.: Elasticsearch) → Jaeger UI (via Jaeger Query).

## Métricas (Prometheus)

### Modelo de dados de métricas do OTel — Instrumentos

Instrumentos **síncronos** (chamados inline com o código, no momento do evento):

| Instrumento | Comportamento | Exemplo |
|---|---|---|
| **Counter** | Monotônico, só aumenta | `http.server.request_count.add(1)` — Prometheus calcula `rate()` |
| **UpDownCounter** | Sobe ou desce | `queue.items.add(1)` / `.add(-1)` — valor instantâneo, não serve p/ taxa |
| **Histogram** | Agrupa medições em buckets pré-definidos | `http.server.duration.record(125.5)` — permite calcular média e percentis (p95, p99) |

Instrumentos **assíncronos/observáveis** (callback chamado periodicamente pelo SDK, para valores já mantidos em algum lugar do código):

| Instrumento | Comportamento | Exemplo |
|---|---|---|
| **Asynchronous Counter** | Callback retorna soma monotônica | tempo total de CPU consumido |
| **Asynchronous UpDownCounter** | Callback retorna valor que sobe/desce | tamanho de um cache |
| **Asynchronous Gauge** | Valor instantâneo não somável | uso de CPU/memória, temperatura |

- **Atributos (labels/dimensões)**: enriquecem métricas para fatiar/filtrar dados (ex.: `http.method`, `http.status_code`). Seguir convenções semânticas (ex.: duração de request HTTP de servidor deve ser `http.server.duration`, um histogram).

### Prometheus

- Sistema de monitoramento/alerta open-source (CNCF, origem SoundCloud), padrão de fato em Kubernetes.
- Modelo **pull**: aplicações expõem `/metrics`; Prometheus "raspa" (scrape) em intervalos regulares (ex.: 15s) — diferente do modelo **push** (app envia ativamente).
- Vantagens do pull: controle centralizado, resiliência (app continua rodando se Prometheus cair), descoberta de serviços (integração nativa com K8s API), simplicidade do alvo (só expõe texto simples).
- Arquitetura: **Servidor Prometheus** (Scraping Engine + **TSDB** + **PromQL Engine** + API HTTP) + **Alertmanager** (agrupa/dedup/roteia alertas para Slack, e-mail, PagerDuty) + **Exporters** (para sistemas que não falam Prometheus nativamente).

### Integrando OTel com Prometheus (push vs. pull)

Duas abordagens para reconciliar o modelo push do OTel com o pull do Prometheus:

1. **SDK Prometheus Exporter na app**: a própria aplicação vira alvo de scraping — SDK expõe `/metrics` internamente. Simples, mas exige expor porta HTTP em cada instância e acopla a app ao Prometheus (trocar de back-end push exige mudar config da app).
2. **OTel Collector como ponte** (recomendada): app usa apenas **OTLP Exporter** para enviar métricas ao Collector; o Collector habilita o **Prometheus Exporter** e expõe `/metrics` nele mesmo; Prometheus raspa o Collector, não cada app. Vantagens: desacoplamento total (app só fala OTLP), gerenciamento centralizado, processamento central (enriquecer/filtrar/agregar antes de expor). Collector também pode operar no sentido inverso (**Prometheus Receiver** raspa alvos existentes e converte para OTLP).

## Logs (Loki)

### Logs estruturados

- Log não estruturado (texto livre) é ruim para máquinas — busca exige regex, lento e frágil.
- Log estruturado (JSON) permite pesquisa eficiente por campo, vira fonte de dados para métricas/visualizações, garante consistência entre serviços. OTel formaliza logging estruturado como padrão.

### LogRecord — modelo de dados de log do OTel

| Campo | Descrição |
|---|---|
| `Timestamp` | momento em que o evento ocorreu |
| `ObservedTimestamp` | momento em que o log foi recebido pelo coletor (diferença = latência do pipeline) |
| `SeverityText` / `SeverityNumber` | rótulo textual (INFO/WARN/ERROR) e numérico (ex.: INFO=9, ERROR=17) |
| `Body` | conteúdo principal (string ou objeto estruturado) |
| `Attributes` | pares chave-valor com metadados do log (`user_id`, `order_id`...) |
| `Resource attributes` | descrevem a entidade que produziu o log (`service.name`, `k8s.pod.name`) |
| **`TraceId` / `SpanId`** | **campo mais importante para correlação** — injetados automaticamente a partir do trace/span ativo no momento do log |

### Correlação entre pilares via TraceId/SpanId

- Do trace lento no Jaeger → pegar TraceId/SpanId → pular direto para os logs daquele span.
- De um log de erro no Loki (contém TraceId) → pular para o trace completo no Jaeger.
- De um pico de métrica de erro no Grafana → usar a janela de tempo para filtrar logs `level="ERROR"` → analisar traces correspondentes.
- Essa navegação fluida entre sinais é o principal benefício de usar OTel para coletar os três pilares de forma unificada.

### Grafana Loki

- Sistema de agregação de logs (Grafana Labs), inspirado no Prometheus. Filosofia: **"indexe os metadados, não os dados"** — ao contrário do Elasticsearch, que indexa texto completo (rápido/flexível, mas caro em CPU/memória/disco).
- Loki agrupa logs em **streams** por um conjunto pequeno de **labels** (os mesmos metadados usados no Prometheus, ex.: `{job="payment-service", namespace="prod"}`), indexando só os labels (índice ordens de magnitude menor). Corpo dos logs é comprimido em **chunks** e armazenado em object storage barato (S3, GCS).
- Busca no LogQL em 2 etapas: (1) seletor de labels filtra rapidamente os streams via índice pequeno; (2) busca por força bruta (grep) nos chunks descomprimidos para filtrar pelo conteúdo (ex.: `|= "user_id=123"`). Trade-off: extremamente eficiente em custo/operação, porém mais lento que indexação total para buscas amplas de texto livre.

## Boas práticas e desafios

### A narrativa da depuração moderna (métrica → trace → log)

1. **O alerta (métrica)**: dashboard mostra pico anômalo (ex.: p99 de latência disparou) → "o que está errado?".
2. **O contexto amplo (trace)**: salta-se da métrica para um trace exemplar daquele período → visualização em cascata (Gantt) mostra onde está o gargalo (ex.: chamada a outro serviço).
3. **A causa raiz (log)**: do span suspeito, via correlação TraceId, salta-se para os logs daquele exato momento → mensagem de erro explica o "por quê" (ex.: "Timeout expired" no pool de conexões).

O TraceId é o fio condutor que costura os três sinais — essa jornada "que → onde → por quê" é o valor real da observabilidade (minutos em vez de horas).

### Boas práticas fundamentais

- **Convenções semânticas**: usar sempre os nomes padronizados do OTel (`http.status_code`, não `response_code` num serviço e outro nome noutro) — permite queries/dashboards unificados entre serviços e entre instrumentação automática e manual.
- **Combinar automática + manual**: automática cobre a estrutura técnica; manual adiciona contexto de negócio (spans/atributos como `customer.tier`, `is_trial_user`).
- **OTel Collector como intermediário obrigatório**: nunca exportar direto da app para o back-end final. Vantagens centralizadas no Collector:
  - **Desacoplamento**: app só fala OTLP; trocar/adicionar back-end é só mudar config do Collector.
  - **Tail-based sampling**: captura 100% dos erros sem sobrecarregar o back-end.
  - **Filtragem de PII**: remove dados pessoais/senhas/API keys antes de saírem da infraestrutura (conformidade LGPD/GDPR).
  - **Enriquecimento de metadados**: região da nuvem, ID do cluster K8s, versão da app.
  - **Eficiência de rede**: agrega, comprime e envia em batches.

### Gerenciamento de cardinalidade

- **Cardinalidade** = número de valores únicos de um atributo/label. Alta cardinalidade é um dos maiores desafios técnicos/custo em observabilidade — cada combinação única de labels cria uma nova série temporal (Prometheus) ou novo stream (Loki).
- Atributos de cardinalidade infinita (`user_id`, `request_id`, `trace_id`) usados como **label** causam "explosão de cardinalidade".
- Regras por sinal:
  - **Métricas (Prometheus)**: nunca usar atributos de alta cardinalidade como label. Usar dimensões de baixa/média cardinalidade (`http_method`, `status_code_class` como `2xx/4xx/5xx`, `service_version`).
  - **Logs (Loki)**: labels só para metadados de infraestrutura (`cluster`, `namespace`, `pod`, `service`) e severidade. Identificadores de alta cardinalidade (`user_id`, `trace_id`) ficam no **corpo** da mensagem, não como label.
  - **Traces (Jaeger)**: traces são projetados para alta cardinalidade — pode-se adicionar `user_id` como atributo de span sem problema (indexação de tracing é otimizada para isso).

### Overhead e amostragem (sampling)

- Instrumentação não é gratuita — consome CPU/memória; instrumentação agressiva em apps de baixa latência pode gerar overhead perceptível.
- Mitigações:
  - **Sampling head-based** no SDK: reduz % de traces processados/exportados desde a origem.
  - **Sampling tail-based** no Collector: combinado ao head-based, garante reter traces importantes (ex.: 100% dos erros) mesmo amostrando agressivamente os bem-sucedidos (ex.: 1–5%).
  - **Ajuste do SDK**: tamanho de lotes, intervalos de exportação, limites de fila.
  - **Collector como agente por nó** (DaemonSet no K8s) em vez de deployment centralizado — distribui carga e minimiza saltos de rede.

### Volume de dados e custo

- Amostragem agressiva de traces bem-sucedidos (mantendo 100% dos erros).
- Políticas de retenção diferenciadas por sinal (ex.: traces detalhados por dias, métricas por meses, logs agregados por até um ano).
- Filtragem no Collector: descartar logs DEBUG em produção, métricas de health checks, spans/logs de baixo valor antes de incorrer em custo no back-end.

### Curva de aprendizado

- Começar simples: um serviço, um objetivo claro, instrumentação automática para ganho rápido.
- Investir em treinamento/documentação; formar "campeões" de observabilidade nas equipes.
- Padronizar dashboards no Grafana como ponto de partida consistente.

### Instrumentação de código legado / terceiros

- **Exporters/agentes** já existentes para sistemas conhecidos (bancos de dados, proxies).
- **Service mesh/proxy** (Istio, Linkerd, NGINX/Envoy) geram spans/métricas sobre o tráfego sem tocar no código do serviço.
- **Logs como última fonte**: usar o Collector para parsear logs de acesso e até gerar métricas a partir deles quando não há outra opção.

### Futuro (contexto, não requisito da fase)

- **eBPF**: observa comportamento do sistema no nível do kernel Linux (syscalls, rede, processos) sem exigir instrumentação de código — complementa o OTel tradicional (eBPF para infraestrutura/rede, instrumentação para contexto de negócio).
- **Continuous profiling** (possível "quarto pilar"): analisa CPU/memória até o nível de linha de código; complementa traces (trace diz *qual span* é lento, profiler diz *qual linha* consome CPU). Visualizado como flame graphs (ex.: Pyroscope/Grafana Phlare).
- **AIOps**: ML/IA sobre dados de observabilidade — detecção de anomalias, correlação automática de eventos, análise preditiva. OTel, ao padronizar os dados, é fonte ideal para alimentar esses modelos.
- **Cultura de observabilidade**: responsabilidade compartilhada ("you build it, you run it, you own it"), desenvolvedores como instrumentadores, dados acessíveis a todos (não só plantão de Ops).

## Aplicação no `oficina-api`

Contexto: `oficina-api` é Spring Boot 3.3 / Java 21. As aulas usam Python/Flask no hands-on, mas os mesmos conceitos (API/SDK, Collector, OTLP, convenções semânticas) se aplicam via o ecossistema **OpenTelemetry Java**. Como observabilidade não é requisito obrigatório da Fase 2 (ver [[00-requisitos-fase2]]), a instrumentação automática via Java Agent é o caminho de melhor custo-benefício para diferencial.

### Auto-instrumentação com o OpenTelemetry Java Agent

- Baixar o `opentelemetry-javaagent.jar` (projeto `opentelemetry-java-instrumentation`) e anexá-lo à JVM via `-javaagent`, sem alterar código:
  ```
  java -javaagent:/otel/opentelemetry-javaagent.jar -jar oficina-api.jar
  ```
- No Dockerfile, isso significa copiar o jar do agent para a imagem e ajustar o `ENTRYPOINT`/`CMD` para incluir a flag `-javaagent`.
- O agent instrumenta automaticamente Spring MVC/WebFlux, JDBC (útil para consultas de OS no banco), clientes HTTP, etc. — cobre o "big picture" (controllers de abertura/consulta/aprovação/listagem de OS) sem esforço manual.
- **Instrumentação manual** (SDK OTel para Java, `opentelemetry-api`/`opentelemetry-sdk`) pode complementar depois com spans de negócio, ex.: span custom para o fluxo de aprovação de orçamento com atributos como `os.status`, `os.id`, seguindo a mesma lógica de "automática como base + manual para contexto de negócio" da Aula 05.

### Variáveis de ambiente típicas (OTel SDK/Agent)

| Variável | Uso |
|---|---|
| `OTEL_SERVICE_NAME` | nome do serviço na telemetria (ex.: `oficina-api`) — vira `service.name` como resource attribute |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | endpoint do OTel Collector (ex.: `http://otel-collector:4317` gRPC, ou `:4318` HTTP) |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `grpc` ou `http/protobuf` |
| `OTEL_TRACES_EXPORTER` / `OTEL_METRICS_EXPORTER` / `OTEL_LOGS_EXPORTER` | `otlp` (enviar ao Collector) ou `none` para desabilitar um pilar |
| `OTEL_RESOURCE_ATTRIBUTES` | atributos extras (ex.: `deployment.environment=dev,service.version=1.0.0`) |
| `OTEL_TRACES_SAMPLER` / `OTEL_TRACES_SAMPLER_ARG` | sampler head-based (ex.: `parentbased_traceidratio` com arg `0.1` para 10%) |

Em todos os casos, a aplicação fala **apenas OTLP** com o Collector — nunca diretamente com Jaeger/Prometheus/Loki, seguindo a boa prática de desacoplamento da Aula 05.

### Encaixe no docker-compose (dev local)

Serviços adicionais ao [[dockerizacao]] existente:

- **`otel-collector`**: recebe OTLP da `oficina-api` (portas 4317/4318), processa (batch, atributos, filtragem de PII) e exporta para os back-ends via `otel-collector-config.yaml`.
- **`jaeger`** (all-in-one): back-end de traces, recebe do Collector via exporter Jaeger.
- **`prometheus`**: faz *scrape* do endpoint `/metrics` exposto pelo Collector (Prometheus Exporter do Collector) — não da app diretamente.
- **`loki`** + **`grafana`**: agregação de logs (logs da app em JSON, enriquecidos com TraceId/SpanId, encaminhados ao Collector ou direto ao Loki) e dashboards, com "Derived Fields" ligando log → trace no Jaeger.
- A `oficina-api` no compose só precisa das env vars da tabela acima apontando para `otel-collector:4317`; nenhum outro serviço da app muda.

### Encaixe no Kubernetes

Referência: [[kubernetes]] para os manifestos base (Deployments, Services, ConfigMaps/Secrets, HPA).

- **Collector como DaemonSet** (recomendado pela Aula 05 para minimizar overhead/saltos de rede): um Collector por nó, app envia para o Collector local (`localhost:4317` via `hostPort` ou serviço local) em vez de um Collector central compartilhado.
- Alternativa mais simples para começar: **Collector como Deployment único** + `Service` interno (`otel-collector.observability.svc.cluster.local:4317`), consumido por todos os pods da `oficina-api`.
- Config do Collector (pipelines de receivers/processors/exporters) em um **ConfigMap**, montado como volume no Deployment/DaemonSet do Collector.
- Variáveis `OTEL_*` da tabela acima injetadas no Deployment da `oficina-api` via `env`/`envFrom` (ConfigMap), com `OTEL_EXPORTER_OTLP_ENDPOINT` apontando para o Service do Collector.
- Back-ends (Jaeger, Prometheus, Loki, Grafana) podem rodar como Deployments/Services no mesmo cluster (namespace `observability`, por exemplo) — fora do escopo obrigatório do HPA/CI-CD da fase, mas reforça o desenho de arquitetura citado no README esperado.
- Este bloco de observabilidade é um **diferencial** da Fase 2: não bloqueia a entrega obrigatória (app + Docker + K8s + Terraform + CI/CD), mas soma valor ao demonstrar maturidade de infraestrutura no vídeo/README.
