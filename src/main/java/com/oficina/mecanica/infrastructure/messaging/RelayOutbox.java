package com.oficina.mecanica.infrastructure.messaging;

import com.oficina.mecanica.infrastructure.observability.MetricasMensageria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class RelayOutbox {

    private static final Logger log = LoggerFactory.getLogger(RelayOutbox.class);
    private static final int TAMANHO_MAXIMO_ERRO = 500;

    private final OutboxJpaRepository repository;
    private final EnviadorMensagens enviador;
    private final MetricasMensageria metricas;
    private final PropriedadesMensageria propriedades;
    private final TransactionTemplate transacao;
    private final Clock relogio;

    public RelayOutbox(OutboxJpaRepository repository, EnviadorMensagens enviador, MetricasMensageria metricas,
                       PropriedadesMensageria propriedades, PlatformTransactionManager transactionManager,
                       Clock relogio) {
        this.repository = repository;
        this.enviador = enviador;
        this.metricas = metricas;
        this.propriedades = propriedades;
        this.transacao = new TransactionTemplate(transactionManager);
        this.relogio = relogio;
    }

    @Scheduled(fixedDelayString = "${app.mensageria.relay.intervalo:500}")
    public void publicarPendentes() {
        transacao.executeWithoutResult(status -> {
            for (var linha : repository.reivindicarPendentes(propriedades.relay().lote())) {
                publicar(linha);
            }
        });
    }

    private void publicar(OutboxJpaEntity linha) {
        try {
            enviador.enviar(linha);
            linha.setPublicadaEm(LocalDateTime.now(relogio));
            metricas.registrarPublicacao(linha.getTipo());
        } catch (RuntimeException e) {
            linha.setTentativas(linha.getTentativas() + 1);
            linha.setUltimoErro(truncar(String.valueOf(e.getMessage())));
            log.atWarn()
                .addKeyValue(ContextoMensagem.MENSAGEM_ID, linha.getId())
                .addKeyValue(ContextoMensagem.OS_ID, linha.getOsId())
                .log("Falha ao publicar {} na fila {} (tentativa {}): {}", linha.getTipo(), linha.getFila(),
                    linha.getTentativas(), e.getMessage());
        }
    }

    private static String truncar(String texto) {
        return texto.length() <= TAMANHO_MAXIMO_ERRO ? texto : texto.substring(0, TAMANHO_MAXIMO_ERRO);
    }
}
