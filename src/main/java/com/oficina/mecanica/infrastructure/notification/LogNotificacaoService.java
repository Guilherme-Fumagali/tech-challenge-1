package com.oficina.mecanica.infrastructure.notification;

import com.oficina.mecanica.application.port.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implementação stub do canal de notificação.
 *
 * O canal concreto (email, SMS, push) é uma decisão de negócio ainda aberta —
 * identificada como Hot Spot no Event Storming. Substituir esta classe por um
 * adaptador real (ex: SendGrid, AWS SES, Twilio) não afeta nenhuma regra de negócio.
 */
@Component
public class LogNotificacaoService implements NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(LogNotificacaoService.class);

    @Override
    public void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal) {
        log.info("[NOTIFICAÇÃO] Orçamento aguardando aprovação — OS={} | Cliente={} | Total=R$ {}",
            osId, clienteId, valorTotal);
    }
}
