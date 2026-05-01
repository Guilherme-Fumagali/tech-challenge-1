package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.domain.entity.Veiculo;

import java.util.UUID;

public record VeiculoResponse(
    UUID id,
    String placa,
    String marca,
    String modelo,
    int anoFabricacao,
    UUID clienteId
) {
    public static VeiculoResponse from(Veiculo v) {
        return new VeiculoResponse(v.getId(), v.getPlaca().getValor(),
            v.getMarca(), v.getModelo(), v.getAnoFabricacao(), v.getClienteId());
    }
}
