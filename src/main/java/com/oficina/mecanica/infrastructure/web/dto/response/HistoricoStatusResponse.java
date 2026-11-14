package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.domain.entity.HistoricoStatus;
import com.oficina.mecanica.domain.valueobject.StatusOS;

import java.time.LocalDateTime;

public record HistoricoStatusResponse(
    StatusOS statusOrigem,
    StatusOS statusDestino,
    String evento,
    LocalDateTime ocorridoEm
) {
    public static HistoricoStatusResponse from(HistoricoStatus historico) {
        return new HistoricoStatusResponse(historico.statusOrigem(), historico.statusDestino(), historico.evento(),
            historico.ocorridoEm());
    }
}
