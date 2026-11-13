package com.oficina.mecanica.infrastructure.messaging;

import com.oficina.mecanica.application.port.RegistroMensagensProcessadas;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class RegistroMensagensProcessadasAdapter implements RegistroMensagensProcessadas {

    private final MensagemProcessadaJpaRepository repository;
    private final Clock relogio;

    public RegistroMensagensProcessadasAdapter(MensagemProcessadaJpaRepository repository, Clock relogio) {
        this.repository = repository;
        this.relogio = relogio;
    }

    @Override
    public boolean registrarSeInedita(UUID mensagemId, String tipo) {
        return repository.registrar(mensagemId, tipo, LocalDateTime.now(relogio)) == 1;
    }
}
