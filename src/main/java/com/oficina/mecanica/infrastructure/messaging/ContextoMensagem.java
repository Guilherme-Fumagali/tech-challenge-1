package com.oficina.mecanica.infrastructure.messaging;

import org.slf4j.MDC;

final class ContextoMensagem {

    static final String OS_ID = "os.id";
    static final String SAGA_ID = "saga.id";
    static final String MENSAGEM_ID = "mensagem.id";
    static final String MENSAGEM_TIPO = "mensagem.tipo";
    static final String ATRIBUTO_TIPO = "tipo";
    static final String ATRIBUTO_VERSAO = "versao";

    private ContextoMensagem() {}

    static void limparMdc() {
        MDC.remove(OS_ID);
        MDC.remove(SAGA_ID);
        MDC.remove(MENSAGEM_ID);
    }
}
