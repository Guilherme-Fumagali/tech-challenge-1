package com.oficina.mecanica.domain.valueobject;

import com.oficina.mecanica.domain.exception.DomainException;

import java.util.Objects;

public final class CpfCnpj {

    private final String valor;

    public CpfCnpj(String valor) {
        String limpo = limpar(valor);
        if (limpo.length() == 11) {
            validarCpf(limpo);
        } else if (limpo.length() == 14) {
            validarCnpj(limpo);
        } else {
            throw new DomainException("CPF ou CNPJ inválido: " + valor);
        }
        this.valor = limpo;
    }

    public String getValor() { return valor; }

    public boolean isCpf()  { return valor.length() == 11; }
    public boolean isCnpj() { return valor.length() == 14; }

    private static String limpar(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    private static void validarCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) throw new DomainException("CPF inválido: " + cpf);
        int d1 = digitoCpf(cpf, 10);
        int d2 = digitoCpf(cpf, 11);
        if (cpf.charAt(9) - '0' != d1 || cpf.charAt(10) - '0' != d2) {
            throw new DomainException("CPF inválido: " + cpf);
        }
    }

    private static int digitoCpf(String cpf, int peso) {
        int soma = 0;
        for (int i = 0; i < peso - 1; i++) soma += (cpf.charAt(i) - '0') * (peso - i);
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static void validarCnpj(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) throw new DomainException("CNPJ inválido: " + cnpj);
        int d1 = digitoCnpj(cnpj, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int d2 = digitoCnpj(cnpj, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        if (cnpj.charAt(12) - '0' != d1 || cnpj.charAt(13) - '0' != d2) {
            throw new DomainException("CNPJ inválido: " + cnpj);
        }
    }

    private static int digitoCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (cnpj.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    @Override public boolean equals(Object o) {
        return o instanceof CpfCnpj c && valor.equals(c.valor);
    }
    @Override public int hashCode() { return Objects.hash(valor); }
    @Override public String toString() { return valor; }
}
