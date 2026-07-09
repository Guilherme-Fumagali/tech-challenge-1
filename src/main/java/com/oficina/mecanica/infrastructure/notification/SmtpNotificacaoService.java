package com.oficina.mecanica.infrastructure.notification;

import com.oficina.mecanica.application.port.NotificacaoService;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Envia por e-mail (via SMTP/MailHog em ambiente local) o link simulado de
 * aprovação/reprovação externa de orçamento — POST /api/ordens/{id}/aprovar-externo.
 */
@Component
@ConditionalOnProperty(prefix = "app.notificacao", name = "canal", havingValue = "smtp")
public class SmtpNotificacaoService implements NotificacaoService {

    private final JavaMailSender mailSender;
    private final ClienteRepository clienteRepository;
    private final String remetente;

    public SmtpNotificacaoService(JavaMailSender mailSender,
                                   ClienteRepository clienteRepository,
                                   @Value("${app.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.clienteRepository = clienteRepository;
        this.remetente = remetente;
    }

    @Override
    public void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal, String tokenAprovacao) {
        var cliente = clienteRepository.buscarPorId(clienteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));

        var mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(cliente.getEmail());
        mensagem.setSubject("Orçamento da OS %s aguardando aprovação".formatted(osId));
        mensagem.setText("""
            Olá, %s!

            O orçamento da sua Ordem de Serviço %s está pronto — total de R$ %s.

            Para aprovar ou reprovar, envie uma requisição para:
            POST /api/ordens/%s/aprovar-externo
            Body: {"token": "%s", "decisao": "APROVAR"}  (ou "REPROVAR")

            Exemplo via curl:
            curl -X POST http://localhost:8080/api/ordens/%s/aprovar-externo \\
              -H "Content-Type: application/json" \\
              -d '{"token": "%s", "decisao": "APROVAR"}'
            """.formatted(cliente.getNome(), osId, valorTotal, osId, tokenAprovacao, osId, tokenAprovacao));

        mailSender.send(mensagem);
    }
}
