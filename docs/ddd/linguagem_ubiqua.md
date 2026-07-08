# Linguagem Ubíqua — Sistema de Oficina Mecânica
# Tech Challenge Fase 1 — PosTech FIAP

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
Soma calculada automaticamente de todos os ItemServico e ItemPeca de uma OS.
Enviado ao Cliente para Aprovação antes da execução dos serviços.

### Estoque
Quantidade disponível de uma Peça ou Insumo na oficina.
Decrementado automaticamente quando uma Peça é adicionada a uma OS.
Incrementado manualmente pelo Administrador ao registrar entrada de mercadoria.

### Estoque Mínimo
Quantidade de referência abaixo da qual o sistema emite alerta ao Administrador.

---

## Atores

### Atendente
Funcionário da oficina responsável por recepcionar o Cliente,
identificá-lo, cadastrar o Veículo e abrir a Ordem de Serviço.

### Mecânico
Técnico responsável pelo Diagnóstico do Veículo, identificação dos
Serviços necessários, seleção de Peças e execução dos reparos.

### Administrador (Admin)
Usuário com acesso às funcionalidades administrativas do sistema:
gerenciamento de catálogos, relatórios e configurações.
Autenticado via JWT.

### Cliente (ator)
Proprietário do Veículo que acompanha o status da OS e
aprova ou reprova o Orçamento.

---

## Status da Ordem de Serviço

Os status formam um fluxo linear com dois estados finais.
Nunca usar sinônimos — sempre os termos exatos abaixo.

| Status | Descrição |
|---|---|
| **Recebida** | OS criada pelo Atendente. Veículo chegou à oficina. |
| **Em Diagnóstico** | Mecânico avaliando o Veículo e identificando serviços necessários. |
| **Aguardando Aprovação** | Orçamento gerado e enviado ao Cliente. Aguardando resposta. |
| **Em Execução** | Cliente aprovou o Orçamento. Serviços em andamento. |
| **Finalizada** | Todos os Serviços concluídos. Veículo pronto para retirada. |
| **Entregue** | Veículo devolvido ao Cliente. Estado final positivo. |
| **Cancelada** | Cliente reprovou o Orçamento. Estado final negativo. |

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
Só pode ocorrer quando a OS está em status "Aguardando Aprovação".

### Reprovação
Ação do Cliente que recusa o Orçamento, levando a OS ao status "Cancelada".

### Tempo Médio de Execução
Métrica calculada pela diferença entre DataInicio e DataConclusao
das OS com status "Finalizada" ou "Entregue". Agrupável por tipo de Serviço.

### Estorno de Estoque
Operação automática que devolve ao Estoque as Peças reservadas em uma OS
quando essa OS é Cancelada.

---

## Regras de Negócio (Invariantes)

- Uma OS só pode avançar de status na sequência definida — nunca retroagir.
- O Orçamento só pode ser gerado quando a OS estiver "Em Diagnóstico"
  e possuir ao menos um ItemServico ou ItemPeca.
- Uma Peça só pode ser adicionada a uma OS se houver Estoque disponível.
- O Estoque é decrementado no momento da adição da Peça à OS, não na execução.
- CPF e CNPJ são validados por dígitos verificadores — formato não é suficiente.
- A placa do Veículo aceita o formato antigo (ABC-1234) e Mercosul (ABC1D23).
- Rotas administrativas exigem JWT válido. A consulta de status da OS é pública.

---

## Termos Proibidos (Anti-padrões de linguagem)

| Não usar | Usar em vez disso |
|---|---|
| Chamado / Ticket | Ordem de Serviço (OS) |
| Pedido | Ordem de Serviço (OS) |
| Produto | Peça ou Insumo |
| Preço atual | Snapshot de Preço |
| Encerrar | Cancelar (reprovação) ou Finalizar (conclusão) |
| Usuário | Cliente, Atendente, Mecânico ou Administrador |
| Deletar OS | (não existe — OS nunca é deletada, apenas Cancelada) |
