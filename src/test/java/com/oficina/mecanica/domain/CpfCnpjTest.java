package com.oficina.mecanica.domain;

import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class CpfCnpjTest {

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725"})
    void deveAceitarCpfValido(String cpf) {
        assertThatNoException().isThrownBy(() -> new CpfCnpj(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-81", "11222333000181"})
    void deveAceitarCnpjValido(String cnpj) {
        assertThatNoException().isThrownBy(() -> new CpfCnpj(cnpj));
    }

    @ParameterizedTest
    @ValueSource(strings = {"000.000.000-00", "11111111111", "123.456.789-00"})
    void deveRejeitarCpfInvalido(String cpf) {
        assertThatThrownBy(() -> new CpfCnpj(cpf))
            .isInstanceOf(DomainException.class);
    }

    @Test
    void deveRejeitarCnpjInvalido() {
        assertThatThrownBy(() -> new CpfCnpj("11.222.333/0001-00"))
            .isInstanceOf(DomainException.class);
    }

    @Test
    void deveIdentificarTipo() {
        var cpf = new CpfCnpj("529.982.247-25");
        assertThat(cpf.isCpf()).isTrue();
        assertThat(cpf.isCnpj()).isFalse();
    }

    @Test
    void deveNormalizarRemovendoMascaras() {
        var cpf = new CpfCnpj("529.982.247-25");
        assertThat(cpf.getValor()).isEqualTo("52998224725");
    }
}
