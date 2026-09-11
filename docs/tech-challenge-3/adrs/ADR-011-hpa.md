# ADR-011 — HPA por CPU e memória

- **Data:** 04/09/2026 (retroativa — implementado na Fase 2)
- **Status:** Aceita

## Contexto

O enunciado da Fase 3 cita explicitamente "uso de HPA" como exemplo de decisão que merece ADR. O recurso **já existe** em `k8s/app/hpa.yaml` desde a Fase 2, com `metrics-server` instalado em `k8s/metrics-server/`, mas a decisão nunca foi registrada.

Esta é uma ADR **retroativa**: documenta escolha já implementada, para que o "porquê" não se perca. A Aula 05 de Documentação trata disso como caso legítimo — sem o registro, quem chega depois não consegue avaliar se as premissas ainda valem.

O requisito de negócio que a originou vem da Fase 2: suportar grandes volumes de ordens de serviço em horários de pico, com escalabilidade dinâmica.

## Decisão

Manter **HorizontalPodAutoscaler** sobre o Deployment `oficina-api`, apoiado no `metrics-server`, com a configuração já existente:

| Parâmetro | Valor atual |
|---|---|
| `minReplicas` | 2 |
| `maxReplicas` | **4** |
| CPU | `averageUtilization: 70` |
| Memória | `averageUtilization: 80` |

Duas métricas em vez de uma só: a aplicação é Java e pode saturar memória sem saturar CPU.

## Alternativas consideradas

**KEDA com métricas de aplicação** — escalar por profundidade de fila ou taxa de requisições, sinais mais próximos da carga real que CPU. Descartada por adicionar um operador ao cluster para um ganho que a carga do projeto não justifica.

**Cluster Autoscaler junto do HPA** — permitiria o HPA passar de 3 réplicas provisionando nós novos. Descartada por custo: mais nós, mais dinheiro, num ambiente que roda sob demanda.

**Escala vertical (VPA) ou réplicas fixas** — descartadas por não atenderem "escalabilidade dinâmica", que é texto do enunciado.

## Consequências

**Positivas**
- Atende o requisito de cluster com escalabilidade sem componente adicional.
- `metrics-server` alimenta tanto o HPA quanto o `kubectl top`, útil na demonstração ao vivo.
- Métricas de CPU e memória são exatamente o que o requisito de monitoramento da Fase 3 pede ([SPEC-03](../../../../artefatos-tech-challenge-3/plano-implementacao/specs/SPEC-03-observabilidade.md)).

**Negativas**
- **CPU e memória são sinais indiretos.** Uma aplicação lenta por espera de I/O no banco não sobe CPU, e o HPA não reage.
- **`maxReplicas: 4` não conversa com `max_size = 3` do node group.** O HPA pode pedir 4 pods enquanto o cluster tem no máximo 3 nós. Não quebra necessariamente — 4 pods cabem em 3 nós se os `requests` permitirem — mas o teto real de escala passa a depender de recurso disponível, não do número declarado no HPA. **Verificar com o `k6-script.js` de `k8s/loadtest/` se a 4ª réplica agenda ou fica `Pending`**; se ficar, alinhar os dois números (subir `max_size` para 4 ou baixar `maxReplicas` para 3) numa ADR de correção.
- Aplicação Java com `-XX:MaxRAMPercentage=70.0` mantém memória alta por design da JVM — a métrica de memória tende a ficar próxima do alvo mesmo ocioso, o que pode gerar escala prematura. Vale validar o comportamento com o `k6-script.js` em `k8s/loadtest/`.
