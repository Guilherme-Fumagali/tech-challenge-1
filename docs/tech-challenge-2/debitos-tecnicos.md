# Débitos Técnicos Conhecidos — Fase 2

Registro versionado das simplificações conscientes. Fonte-de-verdade; o
`documento_entrega.docx` (seção 7) e os slides referenciam este arquivo.

> Os quatro débitos da Fase 1 foram **quitados** na Fase 2 (MapStruct nos mappers,
> construtor de 9 parâmetros → `record DadosOrdemServico`, canal SMTP real de
> notificação, Domain Storytelling). Ver `documento_entrega.docx` seção 2.4.

---

## DT-01 · E-mail de orçamento sem detalhamento de itens

**Status:** aberto · **Prioridade:** baixa · **Risco:** baixo

### Situação atual
O e-mail de aprovação (`SmtpNotificacaoService`) exibe apenas o **valor total** da OS
e os botões Aprovar/Reprovar. O cliente não vê quais serviços e peças compõem o valor.

### Causa raiz
O contrato do port carrega só o total:

```java
// application/port/NotificacaoService.java
void notificarOrcamentoPendente(UUID osId, UUID clienteId,
                                BigDecimal valorTotal, String tokenAprovacao);
```

Sem a lista de itens cruzando a fronteira de notificação, o adapter não tem como
renderizar a composição — mesmo que os itens já existam no agregado `OrdemServico`.

### Melhoria planejada
1. Enriquecer o contrato do port com a composição do orçamento — um DTO de itens:
   `descrição`, `tipo` (serviço/peça), `quantidade`, `preçoUnitário` (snapshot),
   `subtotal`.
2. Acrescentar contexto: veículo (placa/modelo), prazo estimado e validade do orçamento.
3. Renderizar uma tabela no corpo HTML já existente em `SmtpNotificacaoService.corpoHtml`.

### Justificativa de escopo
Adiado por ser cosmético para o requisito (o edital pede notificação de aprovação, não
detalhamento). Os itens já vivem no agregado; a mudança propaga dados por uma fronteira
existente, **sem tocar em regra de domínio**. Custo e risco baixos.

### Impacto do débito
Menor transparência ao cliente e potencial reprovação de orçamento por falta de
informação — o cliente aprova/reprova sabendo só o total.
