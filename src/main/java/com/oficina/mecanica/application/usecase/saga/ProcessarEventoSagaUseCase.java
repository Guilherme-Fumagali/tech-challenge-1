package com.oficina.mecanica.application.usecase.saga;

import com.oficina.mecanica.application.port.ComandoAEnviar;
import com.oficina.mecanica.application.port.MetricasSaga;
import com.oficina.mecanica.application.port.PublicadorMensagens;
import com.oficina.mecanica.application.port.RegistroMensagensProcessadas;
import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.application.usecase.ordemservico.CompensacaoEstornoEstoque;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.exception.SagaNaoEncontradaException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.SagaOrdemServicoRepository;
import com.oficina.mecanica.domain.saga.AcaoOrdemServico;
import com.oficina.mecanica.domain.saga.DecisaoSaga;
import com.oficina.mecanica.domain.saga.EstadoSaga;
import com.oficina.mecanica.domain.saga.EventoSaga;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class ProcessarEventoSagaUseCase {

    private final SagaOrdemServicoRepository sagaRepository;
    private final OrdemServicoRepository osRepository;
    private final CompensacaoEstornoEstoque estornoEstoque;
    private final MontadorDadosComando montadorDados;
    private final PublicadorMensagens publicador;
    private final RegistroMensagensProcessadas mensagensProcessadas;
    private final MetricasSaga metricas;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final ConfiguracaoSaga configuracao;
    private final Clock relogio;

    public ProcessarEventoSagaUseCase(SagaOrdemServicoRepository sagaRepository,
                                      OrdemServicoRepository osRepository,
                                      CompensacaoEstornoEstoque estornoEstoque,
                                      MontadorDadosComando montadorDados,
                                      PublicadorMensagens publicador,
                                      RegistroMensagensProcessadas mensagensProcessadas,
                                      MetricasSaga metricas,
                                      UnidadeDeTrabalho unidadeDeTrabalho,
                                      ConfiguracaoSaga configuracao,
                                      Clock relogio) {
        this.sagaRepository = sagaRepository;
        this.osRepository = osRepository;
        this.estornoEstoque = estornoEstoque;
        this.montadorDados = montadorDados;
        this.publicador = publicador;
        this.mensagensProcessadas = mensagensProcessadas;
        this.metricas = metricas;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
        this.configuracao = configuracao;
        this.relogio = relogio;
    }

    public ResultadoConsumo executar(UUID osId, EventoSaga evento) {
        return unidadeDeTrabalho.executar(() -> {
            if (!mensagensProcessadas.registrarSeInedita(evento.mensagemId(), evento.tipo().nome())) {
                return ResultadoConsumo.DUPLICADA;
            }
            var saga = sagaRepository.buscarPorOsId(osId).orElseThrow(() -> new SagaNaoEncontradaException(osId));
            var os = osRepository.buscarPorId(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
            var agora = Instant.now(relogio);

            var decisao = saga.aplicar(evento, os.possuiItens(), agora);
            var ordemAlterada = aplicarNaOrdem(os, decisao, evento);
            if (decisao.executaCompensacaoLocal()) {
                ordemAlterada |= estornoEstoque.aplicar(os, saga.getMotivoCompensacao().name());
            }
            if (ordemAlterada) {
                osRepository.salvar(os);
            }
            enviarComandos(saga, decisao, os, evento.mensagemId(), agora);
            sagaRepository.salvar(saga);
            registrarMetricas(saga, decisao);
            return decisao.aplicada() ? ResultadoConsumo.APLICADA : ResultadoConsumo.IGNORADA;
        });
    }

    public ResultadoConsumo rejeitar(UUID osId, UUID mensagemId, String tipo, String motivo) {
        return unidadeDeTrabalho.executar(() -> {
            if (!mensagensProcessadas.registrarSeInedita(mensagemId, tipo)) {
                return ResultadoConsumo.DUPLICADA;
            }
            sagaRepository.buscarPorOsId(osId).ifPresent(saga -> {
                saga.registrarEventoRejeitado(tipo, mensagemId, motivo, Instant.now(relogio));
                sagaRepository.salvar(saga);
            });
            return ResultadoConsumo.REJEITADA;
        });
    }

    private boolean aplicarNaOrdem(OrdemServico os, DecisaoSaga decisao, EventoSaga evento) {
        var instante = evento.instanteDoFatoOuOcorrencia();
        switch (decisao.acaoOrdemServico()) {
            case REGISTRAR_INICIO_DIAGNOSTICO -> os.registrarInicioDiagnostico();
            case REGISTRAR_ORCAMENTO_ENVIADO -> os.registrarOrcamentoEnviado();
            case REGISTRAR_APROVACAO -> os.registrarAprovacao(instante);
            case REGISTRAR_PAGAMENTO -> os.registrarPagamento(instante);
            case REGISTRAR_INICIO_REPARO -> os.registrarInicioReparo();
            case REGISTRAR_CONCLUSAO -> os.registrarConclusao(instante);
            case NENHUMA -> {
                return false;
            }
        }
        return decisao.acaoOrdemServico() != AcaoOrdemServico.NENHUMA;
    }

    private void enviarComandos(SagaOrdemServico saga, DecisaoSaga decisao, OrdemServico os,
                                UUID causadaPor, Instant agora) {
        for (var comando : decisao.comandos()) {
            var dados = montadorDados.montar(comando, os);
            var mensagemId = UUID.randomUUID();
            publicador.publicar(new ComandoAEnviar(mensagemId, comando.tipo(), saga.getId(), saga.getOsId(),
                causadaPor, dados));
            saga.registrarEnvio(mensagemId, comando.tipo(), dados, agora, configuracao.prazoResposta());
        }
    }

    private void registrarMetricas(SagaOrdemServico saga, DecisaoSaga decisao) {
        if (!decisao.mudouDeEstado()) {
            return;
        }
        metricas.registrarPermanencia(decisao.estadoAnterior(), decisao.permanenciaNoEstadoAnterior());
        if (decisao.estadoAtual() == EstadoSaga.CONCLUIDA) {
            metricas.registrarConclusao(saga.getId());
        } else if (decisao.estadoAtual() == EstadoSaga.COMPENSADA) {
            metricas.registrarCompensacao(saga.getId(), saga.getMotivoCompensacao());
        } else if (decisao.estadoAtual() == EstadoSaga.FALHA) {
            metricas.registrarFalha(saga.getId(), decisao.estadoAnterior(), decisao.detalhe());
        }
    }
}
