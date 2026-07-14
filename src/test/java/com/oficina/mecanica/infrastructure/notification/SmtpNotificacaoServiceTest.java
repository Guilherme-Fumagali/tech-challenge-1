package com.oficina.mecanica.infrastructure.notification;

import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpNotificacaoServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock ClienteRepository clienteRepository;

    SmtpNotificacaoService service;

    @BeforeEach
    void setUp() {
        service = new SmtpNotificacaoService(
            mailSender, clienteRepository, "oficina@example.com", "http://oficina.example");
    }

    @Test
    void deveEnviarEmailComBotoesParaClienteExistente() throws Exception {
        var clienteId = UUID.randomUUID();
        var osId = UUID.randomUUID();
        var cliente = new Cliente(clienteId, new CpfCnpj("529.982.247-25"), "João", "joao@e.com", "11999");

        when(clienteRepository.buscarPorId(clienteId)).thenReturn(Optional.of(cliente));
        var mime = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mime);

        service.notificarOrcamentoPendente(osId, clienteId, new BigDecimal("150.00"), "token-abc");

        verify(mailSender).send(mime);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("joao@e.com");
        assertThat(mime.getFrom()[0].toString()).isEqualTo("oficina@example.com");

        var html = (String) mime.getContent();
        assertThat(html)
            .contains("token-abc")
            .contains(osId.toString())
            .contains("/aprovar-externo?token=token-abc")
            .contains("/reprovar-externo?token=token-abc")
            .contains("Aprovar")
            .contains("Reprovar");
    }

    @Test
    void deveLancarExcecaoSeClienteNaoEncontrado() {
        var clienteId = UUID.randomUUID();
        when(clienteRepository.buscarPorId(clienteId)).thenReturn(Optional.empty());

        var osId = UUID.randomUUID();
        var valor = new BigDecimal("10.00");

        assertThatThrownBy(() -> service.notificarOrcamentoPendente(osId, clienteId, valor, "token"))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
