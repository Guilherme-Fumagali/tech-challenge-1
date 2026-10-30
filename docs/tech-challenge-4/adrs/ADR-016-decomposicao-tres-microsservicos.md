# ADR-016 — Decomposição em três microsserviços por subdomínio

- **Data:** 29/10/2026
- **Status:** Aceita
- **Relacionada:** [ADR-001](../../tech-challenge-3/adrs/ADR-001-quatro-repositorios.md) (a aplicação deixa de ser um único repositório; a localização da infraestrutura é tratada na [ADR-022](./ADR-022-infra-por-servico-pod-identity.md))

## Contexto

O enunciado da Fase 4 exige refatorar a aplicação em no mínimo três microsserviços independentes, cada um com repositório, infraestrutura e banco de dados próprios, e sugere a divisão em Ordem de Serviço, Orçamento e Pagamento, e Execução e Produção.

O `oficina-api` é um monólito em Clean Architecture com um banco PostgreSQL. O Event Storming da Fase 1 já separa os contextos de atendimento (cliente, veículo, OS), orçamento/aprovação e execução, e a aula 1 de Estrutura de Microsserviços II apresenta a decomposição por capacidade de negócio e por subdomínio do DDD. A aula 1 de Estrutura de Microsserviços alerta que uma decomposição errada produz um monólito distribuído.

Dois acoplamentos existentes pesam na divisão:

- A reserva de estoque acontece quando a peça é adicionada à OS. Separar estoque e OS em serviços diferentes transformaria a baixa e o estorno em passos distribuídos.
- O orçamento não é uma entidade: é calculado a partir dos itens da OS, e o token de aprovação vive na própria tabela da OS.

## Decisão

A aplicação é dividida em três serviços:

| Serviço | Repositório | Responsabilidade |
|---|---|---|
| OS Service | `tech-challenge-1` (o monólito, estrangulado) | cliente, veículo, funcionário, catálogo, estoque, OS, histórico, orquestração da saga |
| Billing Service | `oficina-billing-service` | orçamento, envio para aprovação, decisão do cliente, pagamento no Mercado Pago, estorno |
| Execution Service | `oficina-execution-service` | fila de execução, diagnóstico, reparo, comunicação de finalização ou falha |

A migração segue o padrão Strangler Fig citado no capítulo da fase: o repositório do monólito permanece, perde as responsabilidades extraídas e passa a ser o OS Service, preservando o histórico de commits e os testes existentes. Catálogo e estoque permanecem no OS Service para que a reserva de peças e o estorno continuem sendo transações locais.

Cada serviço tem o seu código, `Dockerfile`, manifestos Kubernetes, Terraform do próprio banco e das próprias filas, pipelines e documentação. Não há biblioteca compartilhada entre eles: o código transversal (segurança JWT, tratamento de erros, outbox, observabilidade) é replicado em cada repositório, o que corresponde ao padrão *microservice chassis* da aula 5 de Estrutura de Microsserviços II aplicado por cópia.

## Alternativas consideradas

**Quatro serviços, com catálogo e estoque isolados.** Aproximaria o desenho do exemplo de e-commerce das aulas de SAGA (serviço de estoque com reserva e cancelamento de reserva). Descartada porque a reserva passaria a depender de chamada remota durante o diagnóstico e o estorno viraria mais uma compensação, sem ganho de autonomia para o negócio da oficina, onde catálogo e OS são operados pelas mesmas pessoas.

**Repositório novo para o OS Service, arquivando o monólito.** Descartada porque perderia o histórico das três fases e os testes que sustentam o domínio; o padrão Strangler Fig recomenda evoluir o sistema existente.

**Biblioteca compartilhada (chassis como dependência Maven).** Reduziria a duplicação, mas criaria acoplamento de versão entre os serviços e exigiria um quarto repositório com publicação de artefato. Descartada para esta fase; a duplicação fica registrada como débito.

## Consequências

**Positivas**

- Cada serviço é construído, testado e implantado de forma independente, com pipeline e quality gate próprios.
- O nome do repositório do OS Service é mantido (`tech-challenge-1`), o que preserva os links das entregas anteriores.
- Falha em um serviço não interrompe os outros: as mensagens destinadas a um serviço fora do ar ficam na fila.

**Negativas**

- O fluxo da OS passa a atravessar três serviços e exige gestão transacional distribuída ([ADR-017](./ADR-017-saga-orquestrada.md)).
- Consultas que antes eram um `SELECT` passam a exigir composição de APIs ([ADR-025](./ADR-025-composicao-api-acompanhamento.md)).
- Código transversal replicado em três repositórios; uma correção de segurança precisa ser aplicada três vezes.
