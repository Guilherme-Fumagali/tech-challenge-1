package com.oficina.mecanica.domain.saga;

import java.time.Duration;
import java.util.List;

public record DecisaoSaga(
    ResultadoPasso resultado,
    EstadoSaga estadoAnterior,
    EstadoSaga estadoAtual,
    Duration permanenciaNoEstadoAnterior,
    AcaoOrdemServico acaoOrdemServico,
    List<ComandoSaga> comandos,
    Compensacao compensacaoLocal,
    String detalhe
) {

    public boolean aplicada() {
        return resultado == ResultadoPasso.APLICADO;
    }

    public boolean mudouDeEstado() {
        return estadoAnterior != estadoAtual;
    }

    public boolean executaCompensacaoLocal() {
        return compensacaoLocal != null;
    }
}
