package com.oficina.mecanica.application.usecase.saga;

import java.time.Duration;

public record ConfiguracaoSaga(Duration prazoResposta, int maximoDeReenvios, int loteDeVerificacao) {}
