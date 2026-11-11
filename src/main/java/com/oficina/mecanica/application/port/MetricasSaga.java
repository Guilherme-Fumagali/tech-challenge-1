package com.oficina.mecanica.application.port;

import com.oficina.mecanica.domain.saga.EstadoSaga;
import com.oficina.mecanica.domain.saga.MotivoCompensacao;
import com.oficina.mecanica.domain.saga.TipoComando;

import java.time.Duration;
import java.util.UUID;

public interface MetricasSaga {

    void registrarInicio(UUID sagaId);

    void registrarConclusao(UUID sagaId);

    void registrarCompensacao(UUID sagaId, MotivoCompensacao motivo);

    void registrarFalha(UUID sagaId, EstadoSaga estado, String motivo);

    void registrarReenvio(UUID sagaId, TipoComando comando);

    void registrarPermanencia(EstadoSaga estado, Duration duracao);
}
