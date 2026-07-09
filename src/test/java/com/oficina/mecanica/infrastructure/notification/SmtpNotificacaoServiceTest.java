package com.oficina.mecanica.infrastructure.notification;

import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
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
        service = new SmtpNotificacaoService(mailSender, clienteRepository, "oficina@example.com");
    }

    @Test
    void deveEnviarEmailComTokenParaClienteExistente() {
        var clienteId = UUID.randomUUID();
        var osId = UUID.randomUUID();
        var cliente = new Cliente(clienteId, new CpfCnpj("529.982.247-25"), "João", "joao@e.com", "11999");

        when(clienteRepository.buscarPorId(clienteId)).thenReturn(Optional.of(cliente));

        service.notificarOrcamentoPendente(osId, clienteId, new BigDecimal("150.00"), "token-abc");

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        var mensagem = captor.getValue();
        assertThat(mensagem.getTo()).containsExactly("joao@e.com");
        assertThat(mensagem.getFrom()).isEqualTo("oficina@example.com");
        assertThat(mensagem.getText()).contains("token-abc").contains(osId.toString());
    }

    @Test
    void deveLancarExcecaoSeClienteNaoEncontrado() {
        var clienteId = UUID.randomUUID();
        when(clienteRepository.buscarPorId(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.notificarOrcamentoPendente(
            UUID.randomUUID(), clienteId, new BigDecimal("10.00"), "token"))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
