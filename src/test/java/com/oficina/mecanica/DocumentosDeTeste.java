package com.oficina.mecanica;

import java.util.concurrent.ThreadLocalRandom;

public final class DocumentosDeTeste {

    private static final String LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private DocumentosDeTeste() {}

    public static String cpfValido() {
        var aleatorio = ThreadLocalRandom.current();
        var digitos = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                digitos[i] = aleatorio.nextInt(10);
            }
        } while (todosIguais(digitos));
        digitos[9] = digitoVerificador(digitos, 9);
        digitos[10] = digitoVerificador(digitos, 10);
        var cpf = new StringBuilder();
        for (int d : digitos) {
            cpf.append(d);
        }
        return cpf.toString();
    }

    public static String placaValida() {
        var aleatorio = ThreadLocalRandom.current();
        return "" + letra(aleatorio) + letra(aleatorio) + letra(aleatorio)
            + aleatorio.nextInt(10) + letra(aleatorio) + aleatorio.nextInt(10) + aleatorio.nextInt(10);
    }

    private static char letra(ThreadLocalRandom aleatorio) {
        return LETRAS.charAt(aleatorio.nextInt(LETRAS.length()));
    }

    private static boolean todosIguais(int[] digitos) {
        for (int i = 1; i < 9; i++) {
            if (digitos[i] != digitos[0]) {
                return false;
            }
        }
        return true;
    }

    private static int digitoVerificador(int[] digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += digitos[i] * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
