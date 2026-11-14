# Documentação da Fase 4

Artefatos de arquitetura do Tech Challenge Fase 4: refatoração em microsserviços com saga orquestrada. O OS Service é este repositório; o Billing Service e o Execution Service têm repositórios próprios.

| Artefato | Local |
|---|---|
| Relatório de entrega | [`documento_entrega.pdf`](documento_entrega.pdf) ([`.docx`](documento_entrega.docx)) |
| Apresentação | [`apresentacao.pdf`](apresentacao.pdf) ([`.pptx`](apresentacao.pptx)) |
| ADRs 016 a 023, com índice e relação com as ADRs da Fase 3 | [`adrs/`](adrs/README.md) |
| Domain Storytelling do fluxo distribuído | [`diagramas/domain-storytelling.md`](diagramas/domain-storytelling.md) |
| Débitos técnicos da fase e situação dos anteriores | [`debitos-tecnicos.md`](debitos-tecnicos.md) |
| Contratos do OS Service: OpenAPI e AsyncAPI | [`../openapi.json`](../openapi.json), [`../asyncapi.yaml`](../asyncapi.yaml) |
| Requisições do fluxo completo nos três serviços | [`../demo.http`](../demo.http) |
| Estratégia de testes | [`../testes.md`](../testes.md) |
| Runbooks dos alertas | [`../runbooks/`](../runbooks/README.md) |

## Repositórios

| Repositório | Papel |
|---|---|
| [tech-challenge-1](https://github.com/Guilherme-Fumagali/tech-challenge-1) | OS Service: cadastro, catálogo, estoque, ordem de serviço e orquestração da saga |
| [oficina-billing-service](https://github.com/Guilherme-Fumagali/oficina-billing-service) | Billing Service: orçamento, aprovação, pagamento no Mercado Pago e estorno |
| [oficina-execution-service](https://github.com/Guilherme-Fumagali/oficina-execution-service) | Execution Service: fila de execução, diagnóstico e reparo |
| [oficina-auth-lambda](https://github.com/Guilherme-Fumagali/oficina-auth-lambda) | Autenticação por CPF com segundo fator |
| [oficina-infra-k8s](https://github.com/Guilherme-Fumagali/oficina-infra-k8s) | Plataforma: rede, EKS, API Gateway, Pod Identity, New Relic |
| [oficina-infra-db](https://github.com/Guilherme-Fumagali/oficina-infra-db) | Banco da Fase 3, migrado para `infra/` deste repositório |

## Diagramas

Fontes em Mermaid (ou SVG) nesta pasta; imagens em [`diagramas/png/`](diagramas/png/).

| Diagrama | Fonte |
|---|---|
| Arquitetura geral: microsserviços, bancos e comunicação | [`c2-arquitetura-geral.md`](diagramas/c2-arquitetura-geral.md) |
| Contexto (C4, nível 1) | [`c1-contexto.md`](diagramas/c1-contexto.md) |
| Componentes do OS Service (C4, nível 3); os dos outros serviços estão no README de cada um | [`c3-os-service.md`](diagramas/c3-os-service.md) |
| Filas e mensagens da saga | [`mapa-mensagens.md`](diagramas/mapa-mensagens.md) |
| Saga: fluxo completo | [`sequencia-saga-fluxo-completo.md`](diagramas/sequencia-saga-fluxo-completo.md) |
| Saga: compensação por pagamento recusado | [`sequencia-compensacao-pagamento-recusado.md`](diagramas/sequencia-compensacao-pagamento-recusado.md) |
| Saga: compensação por falha na execução | [`sequencia-compensacao-falha-execucao.md`](diagramas/sequencia-compensacao-falha-execucao.md) |
| Máquina de estados da saga | [`estados-saga.md`](diagramas/estados-saga.md) |
| Status da ordem de serviço | [`estados-os.md`](diagramas/estados-os.md) |
| Autenticação com segundo fator | [`sequencia-autenticacao-2fa.md`](diagramas/sequencia-autenticacao-2fa.md) |
| Modelo relacional do OS Service e do Billing Service | [`der-os-service.md`](diagramas/der-os-service.md), [`der-billing-service.md`](diagramas/der-billing-service.md) |
| Documento de execução (MongoDB) | [`modelo-documento-execucao.md`](diagramas/modelo-documento-execucao.md) |
| Pipeline de CI/CD de cada serviço | [`pipeline-cicd.md`](diagramas/pipeline-cicd.md) |
| Domain Storytelling | [`domain-storytelling.md`](diagramas/domain-storytelling.md) |

## Requisitos do enunciado e onde são atendidos

| Requisito | Onde |
|---|---|
| Três microsserviços com repositório, infraestrutura e banco próprios | [ADR-016](adrs/ADR-016-decomposicao-tres-microsservicos.md), [ADR-019](adrs/ADR-019-banco-por-servico.md), [ADR-022](adrs/ADR-022-infra-por-servico-pod-identity.md) |
| Comunicação síncrona e assíncrona, sem acesso a banco de outro serviço | [ADR-016](adrs/ADR-016-decomposicao-tres-microsservicos.md), [ADR-018](adrs/ADR-018-mensageria-sqs.md), [ADR-022](adrs/ADR-022-infra-por-servico-pod-identity.md), [ADR-023](adrs/ADR-023-segundo-fator-autenticacao.md) |
| Saga com compensação e justificativa da estratégia | [ADR-017](adrs/ADR-017-saga-orquestrada.md), [ADR-020](adrs/ADR-020-outbox-consumidor-idempotente.md), README deste repositório |
| Integração com o Mercado Pago | [ADR-021](adrs/ADR-021-mercado-pago-checkout-pro.md), repositório `oficina-billing-service` |
| Testes, BDD, cobertura e qualidade | [`../testes.md`](../testes.md), README de cada serviço |
| CI/CD, deploy em Kubernetes e observabilidade | README de cada serviço, repositório `oficina-infra-k8s` |
