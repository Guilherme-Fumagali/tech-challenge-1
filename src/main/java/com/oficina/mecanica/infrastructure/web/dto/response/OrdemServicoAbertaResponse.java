package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.application.usecase.ordemservico.AberturaOrdemServico;
import com.oficina.mecanica.domain.valueobject.StatusOS;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrdemServicoAbertaResponse(
    UUID id,
    UUID sagaId,
    UUID clienteId,
    UUID veiculoId,
    StatusOS status,
    LocalDateTime dataAbertura
) {
    public static OrdemServicoAbertaResponse from(AberturaOrdemServico abertura) {
        var os = abertura.ordemServico();
        return new OrdemServicoAbertaResponse(os.getId(), abertura.sagaId(), os.getClienteId(), os.getVeiculoId(),
            os.getStatus(), os.getDataAbertura());
    }
}
