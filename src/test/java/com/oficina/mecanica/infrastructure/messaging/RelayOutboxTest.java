package com.oficina.mecanica.infrastructure.messaging;

import com.oficina.mecanica.infrastructure.observability.MetricasMensageria;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelayOutboxTest {

    private static final Instant AGORA = Instant.parse("2026-10-30T14:00:00Z");

    @Mock OutboxJpaRepository repository;
    @Mock EnviadorMensagens enviador;
    @Mock PlatformTransactionManager transactionManager;

    SimpleMeterRegistry registry;
    RelayOutbox relay;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var propriedades = new PropriedadesMensageria("os-service",
            new PropriedadesMensageria.Filas("a", "b", "c", "a"), "a-dlq", new PropriedadesMensageria.Relay(500, 50));
        relay = new RelayOutbox(repository, enviador, new MetricasMensageria(registry), propriedades,
            transactionManager, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void marcaComoPublicadaAposEnviar() {
        var linha = linha("IniciarDiagnostico");
        when(repository.reivindicarPendentes(50)).thenReturn(List.of(linha));

        relay.publicarPendentes();

        verify(enviador).enviar(linha);
        assertThat(linha.getPublicadaEm()).isEqualTo(LocalDateTime.ofInstant(AGORA, ZoneOffset.UTC));
        assertThat(registry.counter("oficina.mensageria.publicadas", "tipo", "IniciarDiagnostico").count())
            .isEqualTo(1);
    }

    @Test
    void falhaNoEnvioRegistraTentativaESegueParaAProxima() {
        var falha = linha("GerarOrcamento");
        var sucesso = linha("IniciarReparo");
        when(repository.reivindicarPendentes(50)).thenReturn(List.of(falha, sucesso));
        doThrow(new IllegalStateException("x".repeat(600))).when(enviador).enviar(falha);

        relay.publicarPendentes();

        assertThat(falha.getPublicadaEm()).isNull();
        assertThat(falha.getTentativas()).isEqualTo(1);
        assertThat(falha.getUltimoErro()).hasSize(500);
        assertThat(sucesso.getPublicadaEm()).isNotNull();
    }

    private static OutboxJpaEntity linha(String tipo) {
        return new OutboxJpaEntity(UUID.randomUUID(), "execution-comandos", tipo, 1, UUID.randomUUID(), "{}", "{}",
            LocalDateTime.ofInstant(AGORA, ZoneOffset.UTC), null, 0, null);
    }
}
