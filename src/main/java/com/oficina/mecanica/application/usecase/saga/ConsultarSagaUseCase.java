package com.oficina.mecanica.application.usecase.saga;

import com.oficina.mecanica.domain.exception.SagaNaoEncontradaException;
import com.oficina.mecanica.domain.repository.SagaOrdemServicoRepository;

import java.util.UUID;

public class ConsultarSagaUseCase {

    private final SagaOrdemServicoRepository sagaRepository;

    public ConsultarSagaUseCase(SagaOrdemServicoRepository sagaRepository) {
        this.sagaRepository = sagaRepository;
    }

    public ResumoSaga executar(UUID osId) {
        var saga = sagaRepository.buscarPorOsId(osId).orElseThrow(() -> new SagaNaoEncontradaException(osId));
        return new ResumoSaga(saga, sagaRepository.listarPassos(saga.getId()));
    }
}
