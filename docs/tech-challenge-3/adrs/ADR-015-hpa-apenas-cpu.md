# ADR-015 — HPA apenas por CPU

- **Data:** 15/09/2026
- **Status:** Aceita
- **Supera:** [ADR-011](./ADR-011-hpa.md) quanto à métrica de memória

## Contexto

A [ADR-011](./ADR-011-hpa.md) definiu o HPA com duas métricas, CPU a 70% e memória a 80% das requests, e registrou como consequência negativa o risco de escala prematura pela memória, recomendando validar o comportamento com o `k6-script.js`.

A validação foi executada em homologação, com a aplicação instrumentada e o `metrics-server` ativo. As medições, com `requests` de 250m de CPU e 512Mi de memória:

| Situação | CPU | Memória | Réplicas |
|---|---|---|---|
| Ociosa, logo após subir | 19% | 81% | 2 |
| Após a carga do k6 (30 usuários virtuais, 5 min) | 56% | 88% | 4 |
| Trinta minutos após o fim da carga | baixa | 88% | 4 |

A memória permanece acima do alvo mesmo sem requisições. A JVM executa com `-XX:MaxRAMPercentage=70.0` e retém o heap já alocado, portanto o uso medido não acompanha a queda da demanda. O efeito prático é que o HPA sobe para o teto de réplicas e não retorna, e a escala deixa de refletir a carga.

## Decisão

O HPA passa a usar apenas a utilização de CPU, mantendo o alvo de 70%, `minReplicas: 2` e `maxReplicas: 4`.

A memória continua sendo observada, mas por outro mecanismo: o alerta `AWS-OficinaAPI-<Ambiente>-K8s-MemoriaPod-Warning`, declarado em `cluster/newrelic.tf`, notifica quando o uso se aproxima do limite do contêiner. Saturação de memória em uma aplicação Java indica dimensionamento incorreto ou vazamento, situações que exigem análise e não mais réplicas.

## Alternativas consideradas

**Aumentar a `request` de memória para 768Mi.** Reduziria a utilização medida para cerca de 55% e adiaria o problema. Descartada porque o heap cresce até o teto configurado sob carga, o que recolocaria a métrica acima do alvo, e porque quatro réplicas passariam a reservar 3 GiB.

**Elevar o alvo de memória para 95%.** Mantém a métrica, mas a torna inócua: o disparo ocorreria apenas quando o contêiner já estivesse perto do `OOMKilled`, tarde demais para a escala ajudar.

**Reduzir `MaxRAMPercentage`.** Diminuiria o heap e a memória residente, ao custo de mais coletas de lixo e menor vazão, sem resolver a característica de retenção da JVM.

## Consequências

**Positivas**

- A escala passa a acompanhar a carga: a validação com o k6 mostrou a subida para quatro réplicas por CPU e o retorno ao mínimo depois da janela de estabilização.
- Uma aplicação ociosa mantém duas réplicas, o que libera capacidade nos dois nós do cluster.

**Negativas**

- Uma carga que sature memória sem saturar CPU não gera réplicas novas. O cenário é coberto pelo alerta de memória, que exige ação humana.
- A decisão depende de a CPU ser o recurso limitante da aplicação, o que vale para o perfil atual, majoritariamente consultas ao banco e serialização JSON. Uma mudança nesse perfil exige reavaliar a métrica.
