package com.oficina.mecanica.application.usecase.saga;

import com.oficina.mecanica.domain.saga.PassoSaga;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;

import java.util.List;

public record ResumoSaga(SagaOrdemServico saga, List<PassoSaga> passos) {}
