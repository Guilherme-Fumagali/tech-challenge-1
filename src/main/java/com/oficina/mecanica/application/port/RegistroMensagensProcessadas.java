package com.oficina.mecanica.application.port;

import java.util.UUID;

public interface RegistroMensagensProcessadas {

    boolean registrarSeInedita(UUID mensagemId, String tipo);
}
