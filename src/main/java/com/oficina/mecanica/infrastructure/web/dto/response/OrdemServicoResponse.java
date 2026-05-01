package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.ItemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.valueobject.StatusOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdemServicoResponse(
    UUID id,
    UUID clienteId,
    UUID veiculoId,
    StatusOS status,
    BigDecimal orcamentoTotal,
    List<ItemServicoResponse> itensServico,
    List<ItemPecaResponse> itensPeca,
    LocalDateTime dataAbertura,
    LocalDateTime dataInicio,
    LocalDateTime dataConclusao
) {
    public record ItemServicoResponse(UUID id, UUID servicoId, String nomeServico,
                                      BigDecimal precoSnapshot, int quantidade, BigDecimal subtotal) {}

    public record ItemPecaResponse(UUID id, UUID pecaId, String nomePeca,
                                   BigDecimal precoSnapshot, int quantidade, BigDecimal subtotal) {}

    public static OrdemServicoResponse from(OrdemServico os) {
        var itensServico = os.getItensServico().stream()
            .map(i -> new ItemServicoResponse(i.getId(), i.getServicoId(), i.getNomeServico(),
                i.getPrecoSnapshot(), i.getQuantidade(), i.getSubtotal()))
            .toList();
        var itensPeca = os.getItensPeca().stream()
            .map(i -> new ItemPecaResponse(i.getId(), i.getPecaId(), i.getNomePeca(),
                i.getPrecoSnapshot(), i.getQuantidade(), i.getSubtotal()))
            .toList();
        return new OrdemServicoResponse(
            os.getId(), os.getClienteId(), os.getVeiculoId(),
            os.getStatus(), os.calcularOrcamento(),
            itensServico, itensPeca,
            os.getDataAbertura(), os.getDataInicio(), os.getDataConclusao()
        );
    }
}
