package com.oficina.mecanica.application.port;

import com.oficina.mecanica.domain.saga.TipoComando;

import java.util.Map;
import java.util.UUID;

public record ComandoAEnviar(
    UUID mensagemId,
    TipoComando tipo,
    UUID sagaId,
    UUID osId,
    UUID causadaPor,
    Map<String, Object> dados
) {}
