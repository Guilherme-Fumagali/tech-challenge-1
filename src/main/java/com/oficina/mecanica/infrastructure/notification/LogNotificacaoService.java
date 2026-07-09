package com.oficina.mecanica.infrastructure.notification;

import com.oficina.mecanica.application.port.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implementação stub do canal de notificação (canal padrão — apenas loga).
 *
 * Ativa por padrão (app.notificacao.canal=log ou ausente). Para envio real por
 * e-mail, ver {@link SmtpNotificacaoService} (app.notificacao.canal=smtp).
 */
@Component
@ConditionalOnProperty(prefix = "app.notificacao", name = "canal", havingValue = "log", matchIfMissing = true)
public class LogNotificacaoService implements NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(LogNotificacaoService.class);

    @Override
    public void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal, String tokenAprovacao) {
        log.info("[NOTIFICAÇÃO] Orçamento aguardando aprovação — OS={} | Cliente={} | Total=R$ {} | Token={}",
            osId, clienteId, valorTotal, tokenAprovacao);
    }
}
