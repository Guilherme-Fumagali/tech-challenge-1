# Documentação da Fase 3

Artefatos de arquitetura do Tech Challenge Fase 3.

| Artefato | Local |
|---|---|
| Relatório de entrega | [`documento_entrega.docx`](documento_entrega.docx) |
| Apresentação | [`apresentacao.pptx`](apresentacao.pptx) |
| RFCs: nuvem, banco de dados e estratégia de autenticação | [`rfcs/`](rfcs/) |
| ADRs: 15 decisões arquiteturais, com índice e processo | [`adrs/`](adrs/README.md) |
| Débitos técnicos | [`debitos-tecnicos.md`](debitos-tecnicos.md) |
| Estratégia de testes da aplicação | [`../testes.md`](../testes.md) |
| Contrato OpenAPI da aplicação | [`../openapi.json`](../openapi.json) |
| Runbooks dos alertas | [`../runbooks/`](../runbooks/README.md) |

## Diagramas

Fontes em Mermaid nesta pasta; imagens renderizadas em [`diagramas/png/`](diagramas/png/).

| Diagrama | Fonte |
|---|---|
| Contexto (C4, nível 1) | [`c1-contexto.md`](diagramas/c1-contexto.md) |
| Contêineres (C4, nível 2): nuvem, APIs, banco e monitoramento | [`c2-containers.md`](diagramas/c2-containers.md) |
| Componentes da aplicação (C4, nível 3) | [`c3-componentes.md`](diagramas/c3-componentes.md) |
| Sequência de autenticação por CPF | [`sequencia-autenticacao.md`](diagramas/sequencia-autenticacao.md) |
| Sequência de abertura de ordem de serviço | [`sequencia-abertura-os.md`](diagramas/sequencia-abertura-os.md) |
| Modelo relacional, com a descrição dos relacionamentos | [`der-modelo-relacional.md`](diagramas/der-modelo-relacional.md) |

## Requisitos do enunciado e onde são atendidos

| Requisito | Onde |
|---|---|
| API Gateway e autenticação por CPF em função serverless | [ADR-003](adrs/ADR-003-autenticacao-cpf-lambda.md), [ADR-004](adrs/ADR-004-api-gateway-http-api.md), [ADR-009](adrs/ADR-009-jwt-hmac-lambda-authorizer.md), [ADR-014](adrs/ADR-014-papeis-cliente-funcionario.md), repositório `oficina-auth-lambda` |
| Quatro repositórios com CI/CD e branches protegidas | [ADR-001](adrs/ADR-001-quatro-repositorios.md), [ADR-012](adrs/ADR-012-ambientes-segregados.md), [ADR-013](adrs/ADR-013-identidade-das-pipelines.md) |
| Banco gerenciado, Kubernetes com escalabilidade e Terraform | [ADR-007](adrs/ADR-007-rede-privada-nat-instance.md), [ADR-010](adrs/ADR-010-postgresql-rds.md), [ADR-011](adrs/ADR-011-hpa.md), repositórios `oficina-infra-k8s` e `oficina-infra-db` |
| Monitoramento com New Relic, dashboards e alertas | [ADR-006](adrs/ADR-006-apm-new-relic.md), runbooks, `oficina-infra-k8s/cluster/newrelic.tf` |
| Documentação arquitetural (componentes, sequência, RFCs, ADRs, ER) | esta pasta |
