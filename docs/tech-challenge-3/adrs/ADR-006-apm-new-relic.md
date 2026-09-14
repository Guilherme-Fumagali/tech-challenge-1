# ADR-006 — New Relic como plataforma de APM

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

O enunciado exige integração com Datadog ou New Relic, monitorando latência de API, recursos do Kubernetes, healthchecks e uptime, com alertas para falhas no processamento de OS e logs estruturados com correlação, além de três dashboards de negócio.

A disciplina de Monitoramento Avançado cobre as duas plataformas com profundidade equivalente e conteúdo espelhado: Datadog nas aulas 2–6 e New Relic nas aulas 7–12.

A aplicação já emite traces via `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` para o Grafana Tempo no ambiente local.

O ambiente é um projeto de estudo, com o EKS já custando US$ 73/mês. Qualquer custo adicional de observabilidade compete com esse orçamento.

## Decisão

New Relic, no free tier.

Instrumentação em três frentes:
- **APM:** agente Java oficial, adicionado ao container via `-javaagent`.
- **Infraestrutura e logs:** integração Kubernetes do New Relic (DaemonSet + Fluent Bit).
- **Métricas de negócio:** Micrometer com `micrometer-registry-otlp` apontando para o endpoint OTLP do New Relic.

## Alternativas consideradas

**Datadog.** Interface de Monitors e Timeboards mais rica, e os Composite Monitors da Aula 05 são um recurso que o New Relic não expõe com a mesma clareza. Descartada pelo modelo de free tier: o APM do Datadog é um trial de 14 dias, depois cobrado por host. Isso obrigaria a planejar a janela de trial para coincidir com a gravação do vídeo, um risco desnecessário em uma entrega com prazo. O New Relic oferece 100 GB/mês de ingestão e 1 full user perpétuos, o que permite manter a instrumentação ativa durante toda a fase.

**Apenas OTLP para o New Relic, sem agente Java.** Padrão aberto, sem acoplamento, e a aplicação já emite OTLP. Descartada porque Apdex, transaction traces e service maps (respectivamente as aulas 07, 08 e 12) vêm prontos no agente e exigiriam construção manual via OTLP. O agente atende ao requisito com menos trabalho e maior aderência ao conteúdo.

**Manter Grafana + Tempo + Loki self-hosted.** Sem custo de licença, mas descartada por violar o requisito, que nomeia Datadog ou New Relic, e por adicionar carga de operação ao cluster.

## Consequências

**Positivas**
- A instrumentação pode permanecer ativa indefinidamente, sem custo e sem prazo de expiração.
- Apdex, service map e Logs in Context são fornecidos pelo agente sem trabalho adicional e cobrem as aulas 07, 08 e 12 na entrega.
- NRQL permite versionar dashboards e alert conditions em Terraform, como recomenda a Aula 10.

**Negativas**
- **Duas vias de telemetria coexistem:** agente Java (APM) e OTLP (métricas de negócio). Isso torna o modelo mais complexo e cria risco de métrica duplicada se o agente também capturar o que o Micrometer envia.
- O `-javaagent` adiciona ~1 s ao startup do pod e consome memória do heap, o que é relevante porque o deployment define `-XX:MaxRAMPercentage=70.0`.
- **A telemetria exige egress para a internet** (`collector.newrelic.com`, `otlp.nr-data.net`). Por esse motivo, subnets privadas sem nenhum NAT são inviáveis; ver [ADR-007](./ADR-007-rede-privada-nat-instance.md).
- O free tier tem teto de 100 GB/mês, e logs em `DEBUG` esgotam essa cota rapidamente. Mitigação em [SPEC-04](../../../../artefatos-tech-challenge-3/plano-implementacao/specs/SPEC-04-logs-estruturados.md) §5.
- O conteúdo de Datadog das aulas 2–6 fica sem aplicação prática.
