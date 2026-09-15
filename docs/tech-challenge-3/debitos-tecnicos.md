# Débitos Técnicos Conhecidos — Fase 3

Registro versionado das simplificações conscientes. Fonte-de-verdade; o
`documento_entrega.docx` (seção 11) e os slides referenciam este arquivo.

> **Débitos da Fase 2:** o DT-01 (e-mail de orçamento sem detalhamento de itens)
> permanece **aberto** — ver seção própria ao final. Os quatro débitos da Fase 1 foram
> quitados na Fase 2.

---

## DT-02 · Segredo JWT simétrico compartilhado entre três componentes

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

O token é assinado com HMAC-SHA256 e o mesmo segredo é lido por três consumidores:
a Lambda emissora, o Lambda authorizer e a aplicação. Todos leem de
`/oficina/<ambiente>/jwt-secret` no SSM Parameter Store.

### Causa raiz

O JWT authorizer nativo do HTTP API é OIDC-compatível: exige issuer, audience e um
endpoint **JWKS** com chaves públicas. Ele não valida HMAC, porque não há chave pública
a publicar quando a assinatura é simétrica. Manter HMAC foi a decisão de menor atrito
sobre o `JwtService` que já existia e funcionava (ADR-009).

### Melhoria planejada

Migrar para **RS256 com JWKS**: chave privada apenas na Lambda emissora, chave pública
publicada num endpoint, e o authorizer nativo do gateway validando sem invocar código.
Elimina o segredo compartilhado e remove um salto de latência.

### Justificativa de escopo

Exigiria hospedar e manter um endpoint JWKS público e reescrever a assinatura nos dois
lados. O ganho é real, mas o passivo só se torna crítico quando o número de validadores
cresce — hoje são três, todos sob o mesmo controle.

### Impacto do débito

Vazamento em **qualquer** um dos três componentes permite forjar tokens válidos para
todos. É o principal passivo de segurança da arquitetura atual.

---

## DT-03 · Revogação de token não é imediata

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

Nem o Lambda authorizer nem a aplicação consultam `clientes.status` a cada requisição —
os dois apenas verificam a assinatura do token. Quando o cache de 300 s do authorizer
expira, ele revalida a assinatura, que continua íntegra.

Portanto a exposição não é o cache: é **o tempo de vida inteiro do token, 15 minutos**.

### Causa raiz

A aplicação virou validadora pura (ADR-003): confia na assinatura e não vai ao banco
para autorizar. O authorizer segue a mesma premissa. Nenhuma das duas camadas foi
desenhada para revogação, e não há mecanismo de revogação no fluxo novo — o refresh
token saiu junto com o `AuthController`.

### Melhoria planejada

Consultar o status na autorização — no authorizer, com cache curto, ou na aplicação a
cada requisição. Alternativa mais barata: reduzir o TTL do token de 900 s para 300 s,
o que corta a janela para um terço sem consulta extra ao banco.

### Justificativa de escopo

Consultar o banco a cada requisição contraria o desenho da ADR-003 e adiciona uma query
por chamada. Aceito para ambiente de estudo, onde bloquear cliente não é operação
frequente nem sensível a tempo.

### Impacto do débito

Cliente bloqueado durante uma sessão ativa continua acessando por **até 15 minutos**,
com todas as permissões que tinha. Bloquear alguém não tem efeito imediato.

---

## DT-04 · NAT instance é ponto único de falha

**Status:** aberto · **Prioridade:** média · **Risco:** baixo

### Situação atual

Uma única instância `t4g.nano` em uma AZ provê a saída de internet das duas subnets
privadas. Se ela cair, os nós de ambas as AZs perdem egress.

### Causa raiz

Decisão de custo, com a conta explícita na ADR-007: NAT instance custa ~US$ 7/mês contra
~US$ 33 do NAT Gateway e ~US$ 58 de VPC interface endpoints em 2 AZs.

### Melhoria planejada

Uma NAT instance por AZ com route table própria, ou migração para NAT Gateway se o
orçamento permitir.

### Justificativa de escopo

Ambiente de estudo, com o ambiente destruído entre sessões de trabalho. O impacto de
uma queda é perda de telemetria e falha de pull de imagem — não indisponibilidade da API,
que continua servindo do que já está no nó.

### Impacto do débito

Telemetria para de fluir e deploy novo falha até a NAT voltar.

---

## DT-05 · Homologação e produção compartilham cluster e banco

**Status:** quitado nesta fase — ver [ADR-012](./adrs/ADR-012-ambientes-segregados.md) · **Prioridade:** média · **Risco:** médio

> **Quitação:** os ambientes passaram a ter cluster, RDS, VPC, state, parâmetros e segredos
> próprios. O custo não dobrou porque produção fica declarada e protegida, mas não
> provisionada. O registro abaixo descreve a situação anterior e fica como histórico. O
> isolamento que ainda falta está em DT-09 e DT-10.

### Situação atual

Os dois ambientes rodam no mesmo cluster EKS, separados por namespace
(`oficina-staging` e `oficina`), e no mesmo RDS, separados por database.

### Causa raiz

Duplicar a infraestrutura custaria mais US$ 73/mês só de EKS control plane, sem ganho
de aprendizado proporcional.

### Melhoria planejada

Contas AWS separadas por ambiente, ou ao menos cluster e instância RDS distintos.

### Justificativa de escopo

Isolamento por namespace e database é o compromisso possível dentro do orçamento da fase.
Está registrado como compromisso, **não como boa prática de produção**.

### Impacto do débito

Um teste de carga em staging consome recursos do mesmo cluster que serve produção. Uma
migration equivocada alcança a mesma instância. Não há isolamento de falha real.

---

## DT-06 · Autenticação por CPF sem segundo fator

**Status:** aberto · **Prioridade:** alta · **Risco:** alto

### Situação atual

Quem informa um CPF válido de cliente ativo recebe um token em nome dele. Não há senha,
código por e-mail nem qualquer segundo fator. Desde a [ADR-014](./adrs/ADR-014-papeis-cliente-funcionario.md),
o funcionário também se autentica apenas pelo CPF, na rota `/auth/funcionarios`, e recebe
acesso de gestão.

### Causa raiz

É o que o enunciado da Fase 3 especifica: *"proteger rotas sensíveis da aplicação com
autenticação via CPF"* e uma function que *"valida o CPF do cliente"*. Foi implementado
como especificado.

### Melhoria planejada

Segundo fator por e-mail ou SMS para clientes, aproveitando o `SmtpNotificacaoService`, e
senha com hash para funcionários.

### Justificativa de escopo

Alterar o fator de autenticação contrariaria o requisito explícito da entrega.

### Impacto do débito

**CPF é identificador público, não segredo.** Quem souber o CPF de um cliente acessa os
dados dele. Mitigações dentro do escopo: resposta 401 idêntica para CPF sem cadastro e cadastro inativo, para
impedir enumeração de CPFs; throttling de 10 req/s na rota `/auth`; token de 15 minutos
sem refresh; e CPF nunca gravado em log — apenas os 3 últimos dígitos. No caso do
funcionário, o impacto é maior, pois o token dá acesso às rotas de gestão; em homologação,
o CPF do funcionário cadastrado não é versionado.

---

## DT-07 · Tabela `refresh_tokens` órfã

**Status:** aberto · **Prioridade:** baixa · **Risco:** nenhum

### Situação atual

A tabela criada pela migration `V2` não é mais lida nem escrita: `RefreshTokenService`
foi removido junto com o `AuthController`.

### Causa raiz

Com a emissão do token migrando para a Lambda (ADR-003), o fluxo de refresh deixou de
existir — o cliente reautentica informando o CPF.

### Melhoria planejada

Migration `V8` derrubando a tabela, depois de confirmar que nenhum ambiente depende dela.

### Justificativa de escopo

`DROP TABLE` é irreversível sem backup, e o ganho é apenas cosmético. Preferimos deixar
a tabela e declarar o débito a executar uma operação destrutiva sem necessidade.

### Impacto do débito

Ruído no schema. Quem ler o banco sem contexto pode supor que há fluxo de refresh ativo.

---

## DT-08 · Validação de CPF duplicada entre repositórios

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

A regra vive em dois lugares: `CpfCnpj` no `oficina-api` e `Cpf` no `oficina-auth-lambda`.

### Causa raiz

A separação em quatro repositórios (ADR-001) cortou o acesso ao código compartilhado, e
publicar um artefato comum exigiria montar CodeArtifact ou GitHub Packages.

### Melhoria planejada

Extrair `oficina-domain-commons` com os value objects compartilhados, publicado e
consumido pelos dois repositórios.

### Justificativa de escopo

Adiado pelo custo de montar e manter o pipeline de publicação do artefato. Como
contenção, `CpfTest` na Lambda **espelha exatamente** os casos-limite do `CpfCnpjTest` da
aplicação — é o que impede os dois validadores de divergirem em silêncio.

### Impacto do débito

Mudança na regra precisa ser aplicada em dois lugares. Divergência não detectada
produziria CPF aceito num componente e recusado no outro.

---

## DT-09 · Ambientes na mesma conta AWS, com role de pipeline compartilhada

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

Homologação e produção têm recursos, rede, state e segredos separados, mas vivem na mesma
conta AWS. Cada repositório usa uma única role de pipeline para os dois ambientes.

### Causa raiz

Separar a role por ambiente dentro de uma conta exigiria condições por tag em cada ação.
Boa parte das ações de EC2, EKS e ELB não as aceita de forma consistente.

### Melhoria planejada

Uma conta AWS por ambiente, via AWS Organizations, com a role de cada pipeline criada
dentro da conta do ambiente.

### Justificativa de escopo

Operar múltiplas contas numa conta de estudo acrescenta esforço sem mudar o que a fase
avalia. A trust já restringe cada environment à própria branch, e produção exige revisor.

### Impacto do débito

Um workflow em `develop` alterado para mirar recursos de produção teria permissão para isso.
O que impede hoje é processo — `develop` protegida, merge por Pull Request — e não o IAM.

---

## DT-10 · Produção preparada e nunca provisionada

**Status:** aberto · **Prioridade:** baixa · **Risco:** baixo

### Situação atual

Produção está declarada em código, com pipeline e aprovação, mas cluster, banco e Lambda
nunca foram provisionados nela.

### Causa raiz

Decisão de custo da [ADR-012](./adrs/ADR-012-ambientes-segregados.md): demonstrar e testar em
homologação.

### Melhoria planejada

Provisionar produção ao menos uma vez antes de qualquer uso real, na ordem documentada, e
destruir em seguida se não houver uso.

### Justificativa de escopo

Homologação exercita o mesmo código, os mesmos workflows e as mesmas policies.

### Impacto do débito

O primeiro provisionamento de produção pode revelar permissão faltando ou diferença de
configuração que homologação não mostrou.

---

## DT-11 · Concorrência reservada da Lambda depende de aumento de cota

**Status:** aberto · **Prioridade:** baixa · **Risco:** baixo

### Situação atual

O template da Lambda aceita o parâmetro `ConcorrenciaReservada`, com padrão 0, que não reserva
concorrência. O teto de dez execuções simultâneas, definido para proteger as conexões do
`db.t4g.micro`, é imposto hoje pelo limite da conta, que é de dez execuções simultâneas para
todas as funções.

### Causa raiz

A AWS exige manter dez execuções não reservadas na conta. Com o limite atual, qualquer reserva
é recusada, e o deploy falha.

### Melhoria planejada

Solicitar aumento da cota *Concurrent executions* em Service Quotas e, depois, implantar com
`ConcorrenciaReservada=10`, o que restabelece o teto explícito por função.

### Justificativa de escopo

Enquanto a cota for de dez execuções, o efeito prático é o mesmo, e o throttling de 10 req/s
nas rotas de autenticação limita a taxa de invocação.

### Impacto do débito

Se a cota for ampliada sem que a reserva seja configurada, a função pode escalar além de dez
execuções simultâneas e pressionar as conexões do banco.

---

## DT-12 · Manifests aplicados por `kubectl` dentro do Terraform

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

Os manifests do cluster são aplicados por um `null_resource` que executa `kubectl apply`, e a
integração Kubernetes do New Relic, por um `helm upgrade`. O Terraform não conhece os objetos
criados: não há plano, nem detecção de desvio, nem remoção do que sai dos arquivos.

### Causa raiz

A escolha evita configurar os providers `kubernetes` e `helm`, que dependem de credenciais do
cluster obtidas no mesmo apply que o cria.

### Melhoria planejada

Migrar para os providers `kubernetes` e `helm`, autenticados pelo `aws_eks_cluster_auth`, de
modo que cada objeto apareça no plano.

### Justificativa de escopo

A abordagem atual é a mesma da Fase 2 e atende ao requisito de provisionamento por código.

### Impacto do débito

Alterações em manifests não apareciam no plano e deixaram de ser aplicadas até que o hash dos
arquivos fosse acrescentado aos gatilhos do recurso. Objetos removidos dos arquivos continuam
existindo no cluster até a destruição do ambiente.

---

## Herdado da Fase 2

## DT-01 · E-mail de orçamento sem detalhamento de itens

**Status:** aberto · **Prioridade:** baixa · **Risco:** baixo

Herdado da Fase 2 sem alteração. O e-mail exibe apenas o valor total da OS e os botões
Aprovar/Reprovar; o cliente não vê quais serviços e peças compõem o valor.

**Por que não foi quitado nesta fase:** o escopo da Fase 3 é infraestrutura, segurança e
observabilidade — nenhum requisito toca o conteúdo da notificação. Quitá-lo junto com uma
eventual migração da notificação para SNS/SES é a oportunidade natural, e continua
adiado enquanto essa migração não estiver no escopo.

Detalhamento completo em `docs/tech-challenge-2/debitos-tecnicos.md`.
