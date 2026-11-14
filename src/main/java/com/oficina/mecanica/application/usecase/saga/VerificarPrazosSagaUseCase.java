package com.oficina.mecanica.application.usecase.saga;

import com.oficina.mecanica.application.port.ComandoAEnviar;
import com.oficina.mecanica.application.port.MetricasSaga;
import com.oficina.mecanica.application.port.PublicadorMensagens;
import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.repository.SagaOrdemServicoRepository;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class VerificarPrazosSagaUseCase {

    static final String MOTIVO_FALHA = "prazo de resposta esgotado após os reenvios";

    private final SagaOrdemServicoRepository sagaRepository;
    private final PublicadorMensagens publicador;
    private final MetricasSaga metricas;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final ConfiguracaoSaga configuracao;
    private final Clock relogio;

    public VerificarPrazosSagaUseCase(SagaOrdemServicoRepository sagaRepository, PublicadorMensagens publicador,
                                      MetricasSaga metricas, UnidadeDeTrabalho unidadeDeTrabalho,
                                      ConfiguracaoSaga configuracao, Clock relogio) {
        this.sagaRepository = sagaRepository;
        this.publicador = publicador;
        this.metricas = metricas;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
        this.configuracao = configuracao;
        this.relogio = relogio;
    }

    public int executar() {
        return unidadeDeTrabalho.executar(() -> {
            var agora = Instant.now(relogio);
            var vencidas = sagaRepository.reivindicarComPrazoVencido(agora, configuracao.loteDeVerificacao());
            vencidas.forEach(saga -> tratar(saga, agora));
            return vencidas.size();
        });
    }

    private void tratar(SagaOrdemServico saga, Instant agora) {
        var comando = saga.getUltimoComandoTipo();
        if (comando == null || saga.reenviosEsgotados(configuracao.maximoDeReenvios())) {
            var estadoAnterior = saga.registrarFalhaPorPrazo(agora);
            sagaRepository.salvar(saga);
            metricas.registrarFalha(saga.getId(), estadoAnterior, MOTIVO_FALHA);
            return;
        }
        var mensagemId = UUID.randomUUID();
        publicador.publicar(new ComandoAEnviar(mensagemId, comando, saga.getId(), saga.getOsId(), null,
            saga.getUltimoComandoDados()));
        saga.registrarReenvio(mensagemId, agora, configuracao.prazoResposta());
        sagaRepository.salvar(saga);
        metricas.registrarReenvio(saga.getId(), comando);
    }
}
