package com.oficina.mecanica.domain.saga;

import java.time.Instant;
import java.util.UUID;

public record PassoSaga(
    DirecaoPasso direcao,
    String tipo,
    UUID mensagemId,
    ResultadoPasso resultado,
    String detalhe,
    Instant ocorridoEm
) {}
