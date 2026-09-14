# ADR-011 — HPA por CPU e memória

- **Data:** 04/09/2026 (retroativa — implementado na Fase 2)
- **Status:** Aceita

## Contexto

O enunciado da Fase 3 cita "uso de HPA" como exemplo de decisão que merece ADR. O recurso já existe em `k8s/app/hpa.yaml` desde a Fase 2, com `metrics-server` instalado em `k8s/metrics-server/`, mas a decisão nunca foi registrada.

Esta é uma ADR retroativa: documenta uma escolha já implementada, para que a justificativa não se perca. A Aula 05 de Documentação trata esse caso como legítimo, pois, sem o registro, quem chega depois não consegue avaliar se as premissas ainda são válidas.

O requisito de negócio que a originou vem da Fase 2: suportar grandes volumes de ordens de serviço em horários de pico, com escalabilidade dinâmica.

## Decisão

Manter HorizontalPodAutoscaler sobre o Deployment `oficina-api`, apoiado no `metrics-server`, com a configuração já existente:

| Parâmetro | Valor atual |
|---|---|
| `minReplicas` | 2 |
| `maxReplicas` | 4 |
| CPU | `averageUtilization: 70` |
| Memória | `averageUtilization: 80` |

São usadas duas métricas em vez de uma porque a aplicação é Java e pode saturar memória sem saturar CPU.

## Alternativas consideradas

**KEDA com métricas de aplicação.** Permitiria escalar por profundidade de fila ou taxa de requisições, sinais mais próximos da carga efetiva que CPU. Descartada por adicionar um operador ao cluster para um ganho que a carga do projeto não justifica.

**Cluster Autoscaler junto do HPA.** Permitiria ao HPA passar de 3 réplicas provisionando nós novos. Descartada por custo: mais nós implicam mais gasto em um ambiente que roda sob demanda.

**Escala vertical (VPA) ou réplicas fixas.** Descartadas por não atenderem à "escalabilidade dinâmica" prevista no texto do enunciado.

## Consequências

**Positivas**
- Atende ao requisito de cluster com escalabilidade sem componente adicional.
- O `metrics-server` alimenta tanto o HPA quanto o `kubectl top`, útil na demonstração ao vivo.
- As métricas de CPU e memória correspondem ao que o requisito de monitoramento da Fase 3 pede ([SPEC-03](../../../../artefatos-tech-challenge-3/plano-implementacao/specs/SPEC-03-observabilidade.md)).

**Negativas**
- **CPU e memória são sinais indiretos.** Uma aplicação lenta por espera de I/O no banco não eleva o uso de CPU, e o HPA não reage.
- **`maxReplicas: 4` não está alinhado com `max_size = 3` do node group.** O HPA pode solicitar 4 pods enquanto o cluster tem no máximo 3 nós. Isso não gera falha necessariamente, pois 4 pods cabem em 3 nós se os `requests` permitirem, mas o teto efetivo de escala passa a depender do recurso disponível, e não do número declarado no HPA. Deve-se verificar com o `k6-script.js` de `k8s/loadtest/` se a 4ª réplica é agendada ou fica `Pending`; nesse caso, alinhar os dois números (subir `max_size` para 4 ou baixar `maxReplicas` para 3) em uma ADR de correção.
- A aplicação Java com `-XX:MaxRAMPercentage=70.0` mantém uso de memória elevado por característica da JVM. A métrica de memória tende a ficar próxima do alvo mesmo com a aplicação ociosa, o que pode gerar escala prematura. Recomenda-se validar o comportamento com o `k6-script.js` em `k8s/loadtest/`.
