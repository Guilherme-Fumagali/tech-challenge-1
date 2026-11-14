package com.oficina.mecanica.domain;

import com.oficina.mecanica.domain.exception.TransicaoInvalidaException;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusOSTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "RECEBIDA, EM_DIAGNOSTICO",
        "EM_DIAGNOSTICO, AGUARDANDO_APROVACAO",
        "EM_DIAGNOSTICO, CANCELADA",
        "AGUARDANDO_APROVACAO, AGUARDANDO_PAGAMENTO",
        "AGUARDANDO_APROVACAO, CANCELADA",
        "AGUARDANDO_PAGAMENTO, EM_EXECUCAO",
        "AGUARDANDO_PAGAMENTO, CANCELADA",
        "EM_EXECUCAO, FINALIZADA",
        "EM_EXECUCAO, CANCELADA",
        "FINALIZADA, ENTREGUE"
    })
    void transicaoPermitida(StatusOS origem, StatusOS destino) {
        assertThatCode(() -> origem.validarTransicaoPara(destino)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} ↛ {1}")
    @CsvSource({
        "RECEBIDA, CANCELADA",
        "RECEBIDA, AGUARDANDO_APROVACAO",
        "EM_DIAGNOSTICO, EM_EXECUCAO",
        "AGUARDANDO_APROVACAO, EM_EXECUCAO",
        "AGUARDANDO_PAGAMENTO, FINALIZADA",
        "EM_EXECUCAO, AGUARDANDO_PAGAMENTO",
        "FINALIZADA, CANCELADA",
        "ENTREGUE, CANCELADA",
        "CANCELADA, RECEBIDA"
    })
    void transicaoProibida(StatusOS origem, StatusOS destino) {
        assertThatThrownBy(() -> origem.validarTransicaoPara(destino))
            .isInstanceOf(TransicaoInvalidaException.class)
            .hasMessageContaining(origem.name())
            .hasMessageContaining(destino.name());
    }
}
