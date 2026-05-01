package com.oficina.mecanica.domain.valueobject;

import com.oficina.mecanica.domain.exception.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Placa {

    // ABC-1234 (antigo) ou ABC1D23 (Mercosul)
    private static final Pattern PADRAO = Pattern.compile(
        "^[A-Z]{3}-?\\d{4}$|^[A-Z]{3}\\d[A-Z]\\d{2}$",
        Pattern.CASE_INSENSITIVE
    );

    private final String valor;

    public Placa(String valor) {
        if (valor == null || !PADRAO.matcher(valor.trim()).matches()) {
            throw new DomainException("Placa inválida: " + valor);
        }
        this.valor = valor.trim().toUpperCase().replace("-", "");
    }

    public String getValor() { return valor; }

    @Override public boolean equals(Object o) {
        return o instanceof Placa p && valor.equals(p.valor);
    }
    @Override public int hashCode() { return Objects.hash(valor); }
    @Override public String toString() { return valor; }
}
