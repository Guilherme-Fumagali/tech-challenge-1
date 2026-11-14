# Débitos Técnicos Conhecidos — Fase 4

Registro versionado das simplificações conscientes da Fase 4. O `documento_entrega.docx` (seção 13) referencia este arquivo. Os débitos da Fase 3 estão em [`../tech-challenge-3/debitos-tecnicos.md`](../tech-challenge-3/debitos-tecnicos.md); a situação de cada um após esta fase está na tabela abaixo.

## Situação dos débitos anteriores

| Débito | Situação na Fase 4 |
|---|---|
| DT-01 · E-mail de orçamento sem detalhamento de itens | **encerrado**: o e-mail do Billing Service lista os itens, o total e a validade |
| DT-02 · Segredo JWT simétrico compartilhado | **aberto e agravado**: o segredo passou a ser lido por cinco componentes (emissor, authorizer e três serviços) |
| DT-03 · Revogação de token não é imediata | aberto, sem alteração |
| DT-04 · NAT instance é ponto único de falha | **aberto e agravado**: a NAT também é o caminho para SQS, SES e Mercado Pago (ver DT-19) |
| DT-05 · Homologação e produção compartilham cluster e banco | encerrado na Fase 3 |
| DT-06 · Autenticação por CPF sem segundo fator | **encerrado** pela ADR-023 |
| DT-07 · Tabela `refresh_tokens` órfã | **encerrado**: tabela e entidade removidas |
| DT-08 · Validação de CPF duplicada entre repositórios | aberto, sem alteração (OS Service e Lambda) |
| DT-09 · Ambientes na mesma conta AWS | aberto; ver também DT-16 |
| DT-10 · Produção preparada e nunca provisionada | **aberto, mantido por custo** |
| DT-11 · Concorrência reservada da Lambda depende de cota | aberto; nenhuma função nova foi criada |
| DT-12 · Manifests aplicados por `kubectl` dentro do Terraform | **encerrado** pela ADR-022 |

## Débitos abertos

| Débito | Risco | Causa | Melhoria planejada |
|---|---|---|---|
| DT-10 · Produção configurada e nunca provisionada | médio | custo: cada hora de ambiente ligado custa ~US$ 0,34, e produção em paralelo dobraria o valor das sessões | provisionar pela mesma esteira (dispatch em `main`) quando houver orçamento |
| DT-13 · Código transversal copiado nos três serviços (JWT, erros, outbox, handler idempotente, métricas) | médio | uma biblioteca comum criaria dependência de versão entre os serviços (ADR-016) | extrair um *chassis* publicado no GitHub Packages |
| DT-14 · Pods e bancos isolados só por credencial, não pela rede | médio | os bancos aceitam o security group do cluster, compartilhado pelos pods | security group por pod e cada banco restrito ao seu serviço |
| DT-15 · Bancos em instância única | médio | custo de Multi-AZ e de réplica do DocumentDB | Multi-AZ e réplica em produção; na falha, as mensagens esperam na fila |
| DT-16 · Pipelines com edição no mesmo namespace | baixo | um namespace para os três serviços | um namespace por serviço |
| DT-17 · Saga em `FALHA` exige intervenção manual | médio | os casos que levam a `FALHA` dependem de análise | endpoint administrativo para reenviar o último comando; hoje o procedimento está no runbook `saga-falha.md` |
| DT-18 · Tráfego entre serviços sem TLS dentro da VPC | baixo | sem service mesh nem certificados internos | mTLS por service mesh |
| DT-19 · Saída para SQS, SES e Mercado Pago pela NAT instance | médio | interface endpoints têm custo por hora (ADR-007) | endpoint de SQS quando o volume justificar; com a NAT fora, o outbox acumula e nada se perde |
| DT-20 · SES em modo sandbox | baixo | sair do sandbox exige solicitação à AWS | pedir acesso de produção antes de usar com clientes reais |
| DT-21 · Outbox e registro de mensagens processadas sem limpeza | baixo | não fazia parte do escopo da fase | job diário que remove registros com mais de 14 dias |
