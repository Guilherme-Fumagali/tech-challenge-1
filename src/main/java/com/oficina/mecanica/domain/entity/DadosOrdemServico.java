package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.StatusOS;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record DadosOrdemServico(
    UUID id,
    UUID clienteId,
    UUID veiculoId,
    StatusOS status,
    List<ItemServico> itensServico,
    List<ItemPeca> itensPeca,
    LocalDateTime dataAbertura,
    LocalDateTime dataAprovacao,
    LocalDateTime dataInicio,
    LocalDateTime dataConclusao,
    boolean excluidaLogicamente,
    LocalDateTime dataExclusaoLogica,
    String tokenAprovacaoExterna,
    LocalDateTime tokenExpiracao,
    LocalDateTime dataUltimaTransicao
) {}
