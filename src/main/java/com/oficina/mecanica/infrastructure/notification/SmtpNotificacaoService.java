package com.oficina.mecanica.infrastructure.notification;

import com.oficina.mecanica.application.port.NotificacaoService;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.application.port.MetricasOrdemServico;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Envia por e-mail (SMTP/MailHog) o orçamento com botões de Aprovar/Reprovar.
 * Os botões apontam para os endpoints GET públicos /api/ordens/{id}/aprovar-externo
 * e /reprovar-externo, que executam a decisão via token de uso único.
 */
@Component
@ConditionalOnProperty(prefix = "app.notificacao", name = "canal", havingValue = "smtp")
public class SmtpNotificacaoService implements NotificacaoService {

    private final JavaMailSender mailSender;
    private final ClienteRepository clienteRepository;
    private final MetricasOrdemServico metricas;
    private final String remetente;
    private final String baseUrl;

    public SmtpNotificacaoService(JavaMailSender mailSender,
                                   ClienteRepository clienteRepository,
                                   MetricasOrdemServico metricas,
                                   @Value("${app.mail.from}") String remetente,
                                   @Value("${app.mail.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.clienteRepository = clienteRepository;
        this.metricas = metricas;
        this.remetente = remetente;
        this.baseUrl = baseUrl;
    }

    @Override
    public void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal, String tokenAprovacao) {
        var cliente = clienteRepository.buscarPorId(clienteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));

        var linkAprovar = "%s/api/ordens/%s/aprovar-externo?token=%s".formatted(baseUrl, osId, tokenAprovacao);
        var linkReprovar = "%s/api/ordens/%s/reprovar-externo?token=%s".formatted(baseUrl, osId, tokenAprovacao);

        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(cliente.getEmail());
            helper.setSubject("Seu orçamento está pronto — OS %s".formatted(osId));
            helper.setText(corpoHtml(cliente.getNome(), osId, valorTotal, linkAprovar, linkReprovar), true);
            mailSender.send(mensagem);
        } catch (MessagingException e) {
            metricas.registrarFalhaIntegracao("email", "montagem_mensagem");
            throw new IllegalStateException("Falha ao montar o e-mail de orçamento.", e);
        }
    }

    private String corpoHtml(String nome, UUID osId, BigDecimal valor, String linkAprovar, String linkReprovar) {
        return """
            <!doctype html>
            <html lang="pt-BR"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body style="margin:0;background:#f1f5f9;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:32px 12px;">
                <tr><td align="center">
                  <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="max-width:480px;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,.08);">
                    <tr><td style="background:#0f172a;padding:24px 32px;">
                      <span style="color:#fff;font-size:18px;font-weight:700;">Oficina Mecânica</span>
                    </td></tr>
                    <tr><td style="padding:32px;">
                      <p style="margin:0 0 8px;font-size:16px;color:#0f172a;">Olá, %s!</p>
                      <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#475569;">
                        O orçamento da sua Ordem de Serviço está pronto. Revise o valor e escolha uma opção:
                      </p>
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border-radius:12px;margin-bottom:28px;">
                        <tr><td style="padding:20px;text-align:center;">
                          <div style="font-size:13px;color:#64748b;text-transform:uppercase;letter-spacing:.5px;">Valor total</div>
                          <div style="font-size:32px;font-weight:800;color:#0f172a;margin-top:4px;">R$ %s</div>
                          <div style="font-size:12px;color:#94a3b8;margin-top:6px;">OS %s</div>
                        </td></tr>
                      </table>
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                          <td width="50%%" style="padding-right:6px;">
                            <a href="%s" style="display:block;text-align:center;background:#16a34a;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:14px 0;border-radius:10px;">&#10003; Aprovar</a>
                          </td>
                          <td width="50%%" style="padding-left:6px;">
                            <a href="%s" style="display:block;text-align:center;background:#dc2626;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:14px 0;border-radius:10px;">&#10005; Reprovar</a>
                          </td>
                        </tr>
                      </table>
                      <p style="margin:24px 0 0;font-size:12px;line-height:1.6;color:#94a3b8;">
                        Os botões não funcionaram? Copie e cole no navegador:<br>
                        Aprovar: <a href="%s" style="color:#64748b;">%s</a><br>
                        Reprovar: <a href="%s" style="color:#64748b;">%s</a>
                      </p>
                    </td></tr>
                    <tr><td style="padding:16px;text-align:center;background:#f8fafc;font-size:12px;color:#94a3b8;">
                      Este link é de uso único. Oficina Mecânica &middot; Tech Challenge
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body></html>
            """.formatted(nome, valor, osId, linkAprovar, linkReprovar, linkAprovar, linkAprovar, linkReprovar, linkReprovar);
    }
}
