# language: pt
Funcionalidade: Ciclo da ordem de serviço coordenado com a execução e o faturamento
  Como atendente da oficina
  Quero que a ordem de serviço avance conforme o diagnóstico, o orçamento, o pagamento e o reparo
  Para entregar o veículo consertado ou, quando algo falha, cancelar a ordem devolvendo peças e pagamento

  Contexto:
    Dado um cliente com um veículo cadastrado
    E o catálogo com um serviço de 120.00 e uma peça de 120.00 com 10 unidades em estoque

  Cenário: Fluxo completo até a entrega do veículo
    Quando o atendente abre a ordem de serviço
    Então a execução recebe o pedido de diagnóstico
    Quando a execução confirma o recebimento do pedido de diagnóstico
    E o mecânico inicia o diagnóstico
    Então a ordem de serviço fica "EM_DIAGNOSTICO"
    Quando o mecânico inclui o serviço e 2 peças na ordem de serviço
    E o mecânico conclui o diagnóstico
    Então o faturamento recebe o pedido de orçamento com total de 360.00
    Quando o orçamento é gerado
    Então a ordem de serviço fica "AGUARDANDO_APROVACAO"
    Quando o cliente aprova o orçamento
    Então a ordem de serviço fica "AGUARDANDO_PAGAMENTO"
    Quando o pagamento é aprovado
    Então a ordem de serviço fica "EM_EXECUCAO"
    E a execução recebe o pedido de reparo
    Quando a execução confirma o recebimento do pedido de reparo
    E o mecânico inicia o reparo
    E o mecânico finaliza o reparo
    Então a ordem de serviço fica "FINALIZADA"
    E o acompanhamento da saga indica "CONCLUIDA"
    Quando o atendente registra a entrega do veículo
    Então a ordem de serviço fica "ENTREGUE"
    E o histórico da ordem de serviço passa por "RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, AGUARDANDO_PAGAMENTO, EM_EXECUCAO, FINALIZADA, ENTREGUE"
    E o estoque da peça fica com 8 unidades

  Cenário: Reprovação do orçamento cancela a execução e devolve as peças
    Dado uma ordem de serviço com orçamento enviado ao cliente e 2 peças reservadas
    Quando o cliente reprova o orçamento
    Então a execução recebe o cancelamento
    Quando a execução confirma o cancelamento
    Então a ordem de serviço fica "CANCELADA"
    E o acompanhamento da saga indica "COMPENSADA"
    E o estoque da peça fica com 10 unidades

  Cenário: Pagamento recusado cancela o orçamento e a execução e devolve as peças
    Dado uma ordem de serviço aguardando pagamento com 2 peças reservadas
    Quando o pagamento é recusado
    Então o faturamento recebe o cancelamento do orçamento
    Quando o faturamento confirma o cancelamento do orçamento
    Então a execução recebe o cancelamento
    Quando a execução confirma o cancelamento
    Então a ordem de serviço fica "CANCELADA"
    E o acompanhamento da saga indica "COMPENSADA"
    E o estoque da peça fica com 10 unidades

  Cenário: Falha na execução estorna o pagamento, cancela o orçamento e devolve as peças
    Dado uma ordem de serviço em execução com 2 peças reservadas
    Quando o mecânico registra falha na execução
    Então o faturamento recebe o pedido de estorno do pagamento
    Quando o faturamento confirma o estorno do pagamento
    Então o faturamento recebe o cancelamento do orçamento
    Quando o faturamento confirma o cancelamento do orçamento
    Então a ordem de serviço fica "CANCELADA"
    E o acompanhamento da saga indica "COMPENSADA"
    E o estoque da peça fica com 10 unidades

  Cenário: Diagnóstico concluído sem itens é reaberto
    Dado uma ordem de serviço em diagnóstico
    Quando o mecânico conclui o diagnóstico
    Então a execução recebe o pedido de reabertura do diagnóstico
    E a ordem de serviço fica "EM_DIAGNOSTICO"
    E o faturamento não recebe pedido de orçamento
    Quando o mecânico inclui o serviço e 1 peça na ordem de serviço
    E o mecânico conclui o diagnóstico
    Então o faturamento recebe o pedido de orçamento com total de 240.00

  Cenário: Aviso repetido de pagamento aprovado não produz efeito duplicado
    Dado uma ordem de serviço aguardando pagamento com 1 peça reservada
    Quando o pagamento é aprovado
    E o mesmo aviso de pagamento aprovado chega novamente
    Então a ordem de serviço fica "EM_EXECUCAO"
    E a execução recebe um único pedido de reparo

  Cenário: Pedido de diagnóstico sem confirmação é reenviado após o prazo
    Quando o atendente abre a ordem de serviço
    Então a execução recebe o pedido de diagnóstico
    Quando a execução não confirma o recebimento dentro do prazo
    Então a execução recebe novamente o pedido de diagnóstico, com nova identificação
    Quando a execução confirma o recebimento do pedido de diagnóstico
    Então o acompanhamento da saga indica "AGUARDANDO_DIAGNOSTICO"
