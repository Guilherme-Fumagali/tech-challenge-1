package com.oficina.mecanica.domain.repository;

import com.oficina.mecanica.domain.saga.PassoSaga;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaOrdemServicoRepository {
    SagaOrdemServico salvar(SagaOrdemServico saga);
    Optional<SagaOrdemServico> buscarPorOsId(UUID osId);
    List<SagaOrdemServico> reivindicarComPrazoVencido(Instant agora, int limite);
    List<PassoSaga> listarPassos(UUID sagaId);
}
