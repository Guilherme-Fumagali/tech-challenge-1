# ADR-007 — Subnets privadas com NAT instance, não NAT Gateway nem VPC endpoints

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

A VPC atual (`10.0.0.0/16`) tem **apenas subnets públicas**, em duas AZs (`us-east-1a` = `10.0.1.0/24`, `us-east-1b` = `10.0.2.0/24`), com `map_public_ip_on_launch = true`. Os nós do EKS e o subnet group do RDS estão nelas. O RDS está protegido por `publicly_accessible = false` e um security group que só aceita tráfego do SG do cluster.

A Aula 04 de Serverless é explícita sobre o risco: um banco exposto publicamente é risco grave de invasão **mesmo com senha forte**, e uma API sem autenticação nas rotas, se estiver em subnet pública, permite que alguém descubra o IP e ataque. O desenho ensinado é subnet privada + load balancer interno + API Gateway via VPC Link.

Três caminhos para dar saída de rede a recursos em subnet privada. Preços de lista em `us-east-1`, para **2 AZs**:

| Opção | Conta | Custo/mês |
|---|---|---|
| **NAT Gateway** | US$ 0,045/h × 730 (1 AZ) | **~US$ 33** + US$ 0,045/GB |
| **VPC interface endpoints** | US$ 0,01/h × **4 endpoints × 2 AZs** × 730 | **~US$ 58** + US$ 0,01/GB |
| **NAT instance `t4g.nano`** | US$ 0,0042/h × 730 + EBS 8 GB + **EIP US$ 0,005/h** | **~US$ 7** |

Interface endpoints são cobrados **por endpoint e por AZ**. Nós privados de EKS precisam de pelo menos `ecr.api`, `ecr.dkr`, `logs` e `sts` — quatro, duplicados nas duas AZs. Só o endpoint de **S3 é gateway, e gateway endpoints são gratuitos**.

Há ainda um bloqueio que nenhum conjunto de endpoints resolve: **a telemetria do New Relic vai para `collector.newrelic.com` e `otlp.nr-data.net`, que são endpoints públicos** ([ADR-006](./ADR-006-apm-new-relic.md)). Sem rota para a internet, não há observabilidade — e observabilidade é requisito obrigatório da fase.

## Decisão

Migrar para **subnets privadas com uma NAT instance `t4g.nano`**.

| Recurso | Subnet |
|---|---|
| NAT instance, NLB interno | pública |
| Nós do EKS, RDS, Lambda com acesso ao banco | **privada** |

Complementos:
- **Gateway endpoint de S3** (gratuito), que atende o tráfego de camadas de imagem do ECR sem passar pela NAT.
- `endpoint_private_access = true` no cluster EKS, mantendo `endpoint_public_access = true` para o `kubectl` da pipeline.
- Novas subnets privadas: `10.0.11.0/24` (`us-east-1a`) e `10.0.12.0/24` (`us-east-1b`).

**Nenhum interface endpoint é criado.** Com a NAT instance no lugar, eles seriam redundantes e mais caros que a própria NAT.

## Alternativas consideradas

**Manter subnets públicas com SG restrito.** Custo zero e é o estado atual. Descartada porque contraria diretamente o desenho ensinado na Aula 04 e porque "subnet pública com SG fechado" é uma postura de segurança pior que "subnet privada" — depende de uma única regra estar correta, sem defesa em profundidade.

**NAT Gateway gerenciado.** Alta disponibilidade real, zero manutenção, escala automática. Descartada por custar **~5× a NAT instance** num ambiente de estudo cujo tráfego de saída é telemetria e pull de imagem.

**Subnets privadas apenas com VPC endpoints, sem NAT.** Foi a intenção inicial. **Descartada por dois motivos objetivos:** (a) sai **mais cara** que o NAT Gateway a 2 AZs — US$ 58 contra US$ 33; (b) **não resolve o problema**, porque a telemetria do New Relic exige saída para a internet pública e não há interface endpoint que atenda um destino de terceiros.

**Manter os nós em subnet pública e privatizar só RDS e Lambda.** Custo zero e resolve o pior risco. Descartada porque o nó do EKS é onde a aplicação roda; deixá-lo com IP público mantém a superfície de ataque que a mudança pretende eliminar.

## Consequências

**Positivas**
- Nós e banco deixam de ter IP público — o desenho passa a ser o da Aula 04 de Serverless.
- Custo de saída de rede cai para ~US$ 7/mês, contra US$ 33 do NAT Gateway ou US$ 58 dos endpoints.
- **Economiza US$ 7,30/mês** de IPv4 público: hoje as subnets usam `map_public_ip_on_launch = true`, então cada nó carrega um EIP cobrado. Em subnet privada, os nós deixam de ter IP público — quase pagando a NAT instance sozinha.
- O gateway endpoint de S3 tira o tráfego de imagem da NAT, reduzindo o gargalo da `t4g.nano`.
- Habilita o padrão API Gateway → VPC Link → NLB interno de [ADR-004](./ADR-004-api-gateway-http-api.md).

**Negativas**
- **A NAT instance é ponto único de falha.** Uma instância, uma AZ. Se cair, os nós de ambas as AZs perdem saída — telemetria para de fluir e pull de imagem falha. Aceito conscientemente: é ambiente de estudo, e o impacto é perda de observabilidade, não indisponibilidade da API.
- **É infraestrutura gerenciada por nós.** Exige AMI com IP forwarding e `source/dest check` desabilitado, além de patching do SO — coisas que o NAT Gateway resolveria sozinho.
- **`t4g.nano` tem banda limitada e é burstable.** Pull simultâneo de imagem em vários nós pode saturar. Mitigado pelo gateway endpoint de S3.
- Migração exige recriar o node group nas subnets novas — **downtime no ambiente AWS** durante a Fase F0.
- O subnet group do RDS muda, o que na AWS implica modificação da instância com janela de indisponibilidade.
