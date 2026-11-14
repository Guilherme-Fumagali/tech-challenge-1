# Linguagem Ubíqua — Sistema de Oficina Mecânica
# Tech Challenge Fases 1 a 4 — PosTech FIAP

Glossário formal dos termos do domínio utilizados em todo o sistema,
documentação e comunicação entre o time de desenvolvimento e os especialistas do negócio.

---

## Entidades do Domínio

### Ordem de Serviço (OS)
Documento central do sistema que registra um atendimento completo de um veículo.
Possui ciclo de vida próprio com status bem definidos.
Nunca chamada de "chamado", "ticket" ou "pedido".

### Cliente
Pessoa física (CPF) ou jurídica (CNPJ) proprietária do veículo.
Identificado obrigatoriamente por CPF ou CNPJ — nunca por nome.

### Veículo
Bem pertencente a um Cliente, identificado pela placa.
Atributos obrigatórios: placa, marca, modelo e ano de fabricação.

### Serviço
Procedimento técnico executado na oficina. Possui nome, descrição,
preço unitário e tempo estimado de execução em horas.
Exemplos: troca de óleo, alinhamento, freios.

### Peça
Item físico utilizado na execução de um Serviço. Possui preço e controle de Estoque.

### Insumo
Material consumível utilizado na execução de Serviços (ex: fluidos, graxas).
Tratado como Peça no sistema para fins de controle de Estoque.

### Item de Serviço (ItemServico)
Registro de um Serviço dentro de uma OS. Captura o preço do Serviço
no momento da criação da OS (Snapshot de Preço), não o preço atual do catálogo.

### Item de Peça (ItemPeca)
Registro de uma Peça dentro de uma OS. Captura o preço da Peça
no momento da criação da OS (Snapshot de Preço).

### Orçamento
Soma de todos os ItemServico e ItemPeca de uma OS. Desde a Fase 4 é mantido pelo
Billing Service, que o recebe do OS Service como snapshot dos itens no pedido de orçamento,
envia ao Cliente para Aprovação e registra a decisão.

### Pagamento
Cobrança do Orçamento aprovado, feita pelo Billing Service no Mercado Pago. O pagamento
aprovado é o ponto a partir do qual a OS segue para o reparo.

### Execução
Registro, no Execution Service, do trabalho do Mecânico sobre a OS: fila, Diagnóstico,
Reparo, apontamentos e eventual falha.

### Estoque
Quantidade disponível de uma Peça ou Insumo na oficina.
Decrementado automaticamente quando uma Peça é adicionada a uma OS.
Incrementado manualmente pelo Funcionário ao registrar entrada de mercadoria.

### Estoque Mínimo
Quantidade de referência abaixo da qual o sistema emite alerta ao Funcionário.

---

## Atores

### Atendente
Funcionário da oficina responsável por recepcionar o Cliente,
identificá-lo, cadastrar o Veículo e abrir a Ordem de Serviço.

### Mecânico
Técnico responsável pelo Diagnóstico do Veículo, identificação dos
Serviços necessários, seleção de Peças e execução dos reparos.

### Funcionário (papel FUNCIONARIO)
Papel do token emitido para Atendente e Mecânico. Dá acesso à gestão de clientes,
veículos, catálogo, estoque, ordens de serviço e relatórios. A autenticação usa CPF e um
código de seis dígitos enviado por e-mail.

### Cliente (ator e papel CLIENTE)
Proprietário do Veículo que acompanha a OS, aprova ou reprova o Orçamento e paga.
Com o papel CLIENTE, consulta apenas as próprias ordens e os próprios veículos.

---

## Status da Ordem de Serviço

Os status formam um fluxo linear com dois estados finais.
Nunca usar sinônimos — sempre os termos exatos abaixo.

| Status | Descrição |
|---|---|
| **Recebida** | OS criada pelo Atendente. Veículo chegou à oficina. |
| **Em Diagnóstico** | Mecânico avaliando o Veículo e identificando serviços necessários. |
| **Aguardando Aprovação** | Orçamento gerado e enviado ao Cliente. Aguardando resposta. |
| **Aguardando Pagamento** | Cliente aprovou o Orçamento. Aguardando a confirmação do Pagamento. |
| **Em Execução** | Pagamento aprovado. Serviços em andamento. |
| **Finalizada** | Todos os Serviços concluídos. Veículo pronto para retirada. |
| **Entregue** | Veículo devolvido ao Cliente. Estado final positivo. |
| **Cancelada** | Orçamento reprovado, expirado ou recusado pelo faturamento, Pagamento recusado ou falha na execução, após as Compensações. Estado final negativo. |

---

## Conceitos de Negócio

### Diagnóstico
Fase técnica em que o Mecânico avalia o Veículo, identifica os problemas
e determina quais Serviços e Peças são necessários.
Diferente de "vistoria" ou "inspeção".

### Snapshot de Preço
Técnica de capturar o preço de um Serviço ou Peça no momento em que é
adicionado à OS. Garante que alterações futuras no catálogo não afetem
Orçamentos já emitidos.

### Aprovação
Ação do Cliente que autoriza a execução dos Serviços descritos no Orçamento.
Só pode ocorrer quando a OS está em status "Aguardando Aprovação". Leva a OS a
"Aguardando Pagamento".

### Reprovação
Ação do Cliente que recusa o Orçamento. Inicia as Compensações e termina com a OS
"Cancelada".

### Saga da Ordem de Serviço
Sequência de passos locais e distribuídos que leva a OS da abertura à finalização,
coordenada pelo OS Service (orquestrador). Cada passo envia um comando a um participante
(Billing ou Execution) e aguarda o evento de resposta. O estado da saga e cada passo
ficam registrados e podem ser consultados por funcionário.

### Compensação
Passo que desfaz o efeito de um passo anterior quando a saga não pode seguir: cancelar a
execução, cancelar o orçamento, estornar o pagamento ou estornar o estoque. A ordem é
sempre a mesma para cada motivo (reprovação, rejeição, pagamento recusado, falha na
execução).

### Prazo de Resposta
Tempo que o orquestrador espera pela resposta de um comando. Vencido o prazo, o comando é
reenviado com nova identificação; esgotados os reenvios, a saga fica em falha e exige
intervenção.

### Tempo Médio de Execução
Métrica calculada pela diferença entre DataInicio e DataConclusao
das OS com status "Finalizada" ou "Entregue". Agrupável por tipo de Serviço.

### Estorno de Estoque
Compensação local, executada pelo OS Service na mesma transação que cancela a OS: devolve
ao Estoque as Peças reservadas. Só ocorre uma vez por OS.

---

## Regras de Negócio (Invariantes)

- Uma OS só pode avançar de status na sequência definida — nunca retroagir. O
  cancelamento é possível a partir de "Em Diagnóstico", "Aguardando Aprovação",
  "Aguardando Pagamento" e "Em Execução", sempre ao fim das Compensações.
- O pedido de Orçamento só é enviado quando o Diagnóstico é concluído e a OS possui ao
  menos um ItemServico ou ItemPeca; sem itens, o Diagnóstico é reaberto.
- Uma Peça só pode ser adicionada a uma OS se houver Estoque disponível.
- O Estoque é decrementado no momento da adição da Peça à OS, não na execução.
- CPF e CNPJ são validados por dígitos verificadores — formato não é suficiente.
- A placa do Veículo aceita o formato antigo (ABC-1234) e Mercosul (ABC1D23).
- Rotas de gestão exigem JWT de Funcionário. A consulta de status da OS é pública e
  devolve apenas identificador, status e data da última mudança.

---

## Termos Proibidos (Anti-padrões de linguagem)

| Não usar | Usar em vez disso |
|---|---|
| Chamado / Ticket | Ordem de Serviço (OS) |
| Pedido | Ordem de Serviço (OS) |
| Produto | Peça ou Insumo |
| Preço atual | Snapshot de Preço |
| Encerrar | Cancelar (compensação) ou Finalizar (conclusão) |
| Usuário | Cliente ou Funcionário (Atendente, Mecânico) |
| Deletar OS | (não existe — OS nunca é deletada, apenas Cancelada) |
