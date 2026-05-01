package com.oficina.mecanica.domain;

import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.valueobject.Placa;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class PlacaTest {

    @ParameterizedTest
    @ValueSource(strings = {"ABC-1234", "ABC1234", "abc1234"})
    void deveAceitarPlacaFormatoAntigo(String placa) {
        assertThatNoException().isThrownBy(() -> new Placa(placa));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC1D23", "abc1d23"})
    void deveAceitarPlacaMercosul(String placa) {
        assertThatNoException().isThrownBy(() -> new Placa(placa));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB1234", "ABCD1234", "ABC12345", "ABC-12D4", ""})
    void deveRejeitarPlacaInvalida(String placa) {
        assertThatThrownBy(() -> new Placa(placa))
            .isInstanceOf(DomainException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc-1234", "abc1234", "ABC-1234"})
    void deveNormalizarParaMaiusculaSemHifen(String placa) {
        var p = new Placa(placa);
        assertThat(p.getValor()).isEqualTo("ABC1234");
    }
}
