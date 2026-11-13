package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.valueobject.StatusOS;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatusPublicoResponse(UUID id, StatusOS status, LocalDateTime atualizadoEm) {

    public static StatusPublicoResponse from(OrdemServico os) {
        return new StatusPublicoResponse(os.getId(), os.getStatus(), os.getDataUltimaTransicao());
    }
}
