package com.oficina.mecanica.infrastructure.agendamento;

import com.oficina.mecanica.application.usecase.saga.VerificarPrazosSagaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgendadorPrazosSaga {

    private static final Logger log = LoggerFactory.getLogger(AgendadorPrazosSaga.class);

    private final VerificarPrazosSagaUseCase verificarPrazos;

    public AgendadorPrazosSaga(VerificarPrazosSagaUseCase verificarPrazos) {
        this.verificarPrazos = verificarPrazos;
    }

    @Scheduled(fixedDelayString = "${app.saga.verificacao-prazos:30000}")
    public void verificar() {
        try {
            var tratadas = verificarPrazos.executar();
            if (tratadas > 0) {
                log.info("{} saga(s) com prazo de resposta vencido tratada(s)", tratadas);
            }
        } catch (RuntimeException e) {
            log.warn("Verificação de prazos da saga falhou e será repetida no próximo ciclo: {}", e.getMessage());
        }
    }
}
