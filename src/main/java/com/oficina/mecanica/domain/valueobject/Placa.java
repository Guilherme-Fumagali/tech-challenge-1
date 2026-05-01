package com.oficina.mecanica.domain.valueobject;

import com.oficina.mecanica.domain.exception.DomainException;

import java.util.regex.Pattern;

public record Placa(String valor) {

    // ABC-1234 (antigo) ou ABC1D23 (Mercosul)
    private static final Pattern PADRAO = Pattern.compile(
        "^[A-Z]{3}-?\\d{4}$|^[A-Z]{3}\\d[A-Z]\\d{2}$",
        Pattern.CASE_INSENSITIVE
    );

    public Placa {
        if (valor == null || !PADRAO.matcher(valor.trim()).matches()) {
            throw new DomainException("Placa inválida: " + valor);
        }
        valor = valor.trim().toUpperCase().replace("-", "");
    }

    public String getValor() { return valor; }
}
