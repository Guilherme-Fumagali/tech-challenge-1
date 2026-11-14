package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.application.usecase.saga.ResumoSaga;
import com.oficina.mecanica.domain.saga.Compensacao;
import com.oficina.mecanica.domain.saga.DirecaoPasso;
import com.oficina.mecanica.domain.saga.EstadoSaga;
import com.oficina.mecanica.domain.saga.MotivoCompensacao;
import com.oficina.mecanica.domain.saga.PassoSaga;
import com.oficina.mecanica.domain.saga.ResultadoPasso;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SagaResponse(
    UUID id,
    UUID osId,
    EstadoSaga estado,
    MotivoCompensacao motivoCompensacao,
    List<Compensacao> compensacoesPendentes,
    String ultimoComando,
    Instant prazoResposta,
    int tentativasReenvio,
    Instant criadaEm,
    Instant atualizadaEm,
    List<PassoResponse> passos
) {
    public record PassoResponse(int sequencia, DirecaoPasso direcao, String tipo, UUID mensagemId,
                                ResultadoPasso resultado, String detalhe, Instant ocorridoEm) {}

    public static SagaResponse from(ResumoSaga resumo) {
        var saga = resumo.saga();
        var passos = new java.util.ArrayList<PassoResponse>();
        for (int i = 0; i < resumo.passos().size(); i++) {
            passos.add(passo(i + 1, resumo.passos().get(i)));
        }
        return new SagaResponse(saga.getId(), saga.getOsId(), saga.getEstado(), saga.getMotivoCompensacao(),
            saga.getCompensacoesPendentes(),
            saga.getUltimoComandoTipo() == null ? null : saga.getUltimoComandoTipo().nome(),
            saga.getPrazoResposta(), saga.getTentativasReenvio(), saga.getCriadaEm(), saga.getAtualizadaEm(),
            List.copyOf(passos));
    }

    private static PassoResponse passo(int sequencia, PassoSaga passo) {
        return new PassoResponse(sequencia, passo.direcao(), passo.tipo(), passo.mensagemId(), passo.resultado(),
            passo.detalhe(), passo.ocorridoEm());
    }
}
