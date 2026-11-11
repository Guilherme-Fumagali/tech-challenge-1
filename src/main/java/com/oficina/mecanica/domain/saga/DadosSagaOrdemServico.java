package com.oficina.mecanica.domain.saga;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record DadosSagaOrdemServico(
    UUID id,
    UUID osId,
    EstadoSaga estado,
    MotivoCompensacao motivoCompensacao,
    List<Compensacao> compensacoesPendentes,
    TipoComando ultimoComandoTipo,
    Map<String, Object> ultimoComandoDados,
    Instant prazoResposta,
    int tentativasReenvio,
    Instant criadaEm,
    Instant atualizadaEm,
    Long versao
) {}
