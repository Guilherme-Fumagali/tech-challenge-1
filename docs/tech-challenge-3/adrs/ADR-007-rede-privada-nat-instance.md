# ADR-007 — Subnets privadas com NAT instance, não NAT Gateway nem VPC endpoints

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

A VPC atual (`10.0.0.0/16`) tem apenas subnets públicas, em duas AZs (`us-east-1a` = `10.0.1.0/24`, `us-east-1b` = `10.0.2.0/24`), com `map_public_ip_on_launch = true`. Os nós do EKS e o subnet group do RDS estão nelas. O RDS está protegido por `publicly_accessible = false` e por um security group que só aceita tráfego do SG do cluster.

A Aula 04 de Serverless trata explicitamente desse risco: um banco exposto publicamente está sujeito a risco grave de invasão, mesmo com senha forte, e uma API sem autenticação nas rotas, se estiver em subnet pública, permite que alguém descubra o IP e a ataque. O desenho apresentado é subnet privada + load balancer interno + API Gateway via VPC Link.

Há três caminhos para prover saída de rede a recursos em subnet privada. Preços de lista em `us-east-1`, para 2 AZs:

| Opção | Conta | Custo/mês |
|---|---|---|
| **NAT Gateway** | US$ 0,045/h × 730 (1 AZ) | ~US$ 33 + US$ 0,045/GB |
| **VPC interface endpoints** | US$ 0,01/h × 4 endpoints × 2 AZs × 730 | ~US$ 58 + US$ 0,01/GB |
| **NAT instance `t4g.nano`** | US$ 0,0042/h × 730 + EBS 8 GB + EIP US$ 0,005/h | ~US$ 7 |

Interface endpoints são cobrados por endpoint e por AZ. Nós privados de EKS precisam de pelo menos `ecr.api`, `ecr.dkr`, `logs` e `sts`, ou seja, quatro endpoints duplicados nas duas AZs. Apenas o endpoint de S3 é do tipo gateway, e gateway endpoints são gratuitos.

Há ainda um bloqueio que nenhum conjunto de endpoints resolve: a telemetria do New Relic é enviada para `collector.newrelic.com` e `otlp.nr-data.net`, que são endpoints públicos ([ADR-006](./ADR-006-apm-new-relic.md)). Sem rota para a internet não há observabilidade, que é requisito obrigatório da fase.

## Decisão

Migrar para subnets privadas com uma NAT instance `t4g.nano`.

| Recurso | Subnet |
|---|---|
| NAT instance, NLB interno | pública |
| Nós do EKS, RDS, Lambda com acesso ao banco | privada |

Complementos:
- Gateway endpoint de S3 (gratuito), que atende o tráfego de camadas de imagem do ECR sem passar pela NAT.
- `endpoint_private_access = true` no cluster EKS, mantendo `endpoint_public_access = true` para o `kubectl` da pipeline.
- Novas subnets privadas: `10.0.11.0/24` (`us-east-1a`) e `10.0.12.0/24` (`us-east-1b`).

Nenhum interface endpoint é criado. Com a NAT instance em uso, eles seriam redundantes e mais caros que a própria NAT.

## Alternativas consideradas

**Manter subnets públicas com SG restrito.** Tem custo zero e corresponde ao estado atual. Descartada porque contraria o desenho apresentado na Aula 04 e porque uma subnet pública com SG fechado oferece postura de segurança inferior à de uma subnet privada, já que depende de uma única regra estar correta, sem defesa em profundidade.

**NAT Gateway gerenciado.** Alta disponibilidade, sem manutenção e com escala automática. Descartada por custar ~5× o valor da NAT instance em um ambiente de estudo cujo tráfego de saída consiste em telemetria e pull de imagem.

**Subnets privadas apenas com VPC endpoints, sem NAT.** Era a intenção inicial. Descartada por dois motivos: (a) o custo é maior que o do NAT Gateway em 2 AZs (US$ 58 contra US$ 33); (b) não resolve o problema, porque a telemetria do New Relic exige saída para a internet pública e nenhum interface endpoint atende um destino de terceiros.

**Manter os nós em subnet pública e privatizar só RDS e Lambda.** Tem custo zero e resolve o risco mais grave. Descartada porque o nó do EKS é onde a aplicação roda; mantê-lo com IP público preserva a superfície de ataque que a mudança pretende eliminar.

**Proxy reverso (Cloudflare, CloudFront) no lugar da NAT.** Não atende ao caso: resolve tráfego de entrada (internet → aplicação), já coberto pelo API Gateway. A NAT atende tráfego de saída (aplicação → New Relic, ECR, SMTP), iniciado pelo pod. Como atuam em direções opostas, uma solução não substitui a outra.

## Consequências

**Positivas**
- Nós e banco deixam de ter IP público, e o desenho passa a seguir o da Aula 04 de Serverless.
- O custo de saída de rede cai para ~US$ 7/mês, contra US$ 33 do NAT Gateway ou US$ 58 dos endpoints.
- **Economia de US$ 7,30/mês** em IPv4 público: atualmente as subnets usam `map_public_ip_on_launch = true`, e cada nó carrega um EIP cobrado. Em subnet privada, os nós deixam de ter IP público, e essa economia cobre quase todo o custo da NAT instance.
- O gateway endpoint de S3 retira o tráfego de imagem da NAT, reduzindo o gargalo da `t4g.nano`.
- Habilita o padrão API Gateway → VPC Link → NLB interno de [ADR-004](./ADR-004-api-gateway-http-api.md).

**Negativas**
- **A NAT instance é ponto único de falha.** Há uma única instância, em uma única AZ. Se ela falhar, os nós de ambas as AZs perdem saída: a telemetria deixa de fluir e o pull de imagem falha. Risco aceito por se tratar de ambiente de estudo, em que o impacto é a perda de observabilidade, e não a indisponibilidade da API.
- **É infraestrutura gerenciada pela equipe.** Exige AMI com IP forwarding e `source/dest check` desabilitado, além de patching do SO, atividades que o NAT Gateway dispensaria.
- **`t4g.nano` tem banda limitada e é burstable.** O pull simultâneo de imagem em vários nós pode saturar a instância. Mitigado pelo gateway endpoint de S3.
- A migração exige recriar o node group nas subnets novas, com downtime no ambiente AWS durante a Fase F0.
- O subnet group do RDS muda, o que na AWS implica modificação da instância com janela de indisponibilidade.
