package com.oficina.mecanica.application;

import com.oficina.mecanica.application.port.ComandoAEnviar;
import com.oficina.mecanica.application.port.MetricasSaga;
import com.oficina.mecanica.application.port.PublicadorMensagens;
import com.oficina.mecanica.application.port.RegistroMensagensProcessadas;
import com.oficina.mecanica.application.usecase.ordemservico.CompensacaoEstornoEstoque;
import com.oficina.mecanica.application.usecase.saga.ConfiguracaoSaga;
import com.oficina.mecanica.application.usecase.saga.MontadorDadosComando;
import com.oficina.mecanica.application.usecase.saga.ProcessarEventoSagaUseCase;
import com.oficina.mecanica.application.usecase.saga.ResultadoConsumo;
import com.oficina.mecanica.domain.entity.DadosOrdemServico;
import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.ItemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.exception.EventoForaDeOrdemException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.exception.SagaNaoEncontradaException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;
import com.oficina.mecanica.domain.repository.SagaOrdemServicoRepository;
import com.oficina.mecanica.domain.saga.DadosSagaOrdemServico;
import com.oficina.mecanica.domain.saga.EstadoSaga;
import com.oficina.mecanica.domain.saga.EtapaExecucao;
import com.oficina.mecanica.domain.saga.EventoSaga;
import com.oficina.mecanica.domain.saga.MotivoCompensacao;
import com.oficina.mecanica.domain.saga.ResultadoPasso;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;
import com.oficina.mecanica.domain.saga.TipoComando;
import com.oficina.mecanica.domain.saga.TipoEvento;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessarEventoSagaUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-10-30T14:00:00Z");
    private static final Duration PRAZO = Duration.ofMinutes(2);

    @Mock SagaOrdemServicoRepository sagaRepository;
    @Mock OrdemServicoRepository osRepository;
    @Mock PecaRepository pecaRepository;
    @Mock MontadorDadosComando montadorDados;
    @Mock PublicadorMensagens publicador;
    @Mock RegistroMensagensProcessadas mensagensProcessadas;
    @Mock MetricasSaga metricas;

    UnidadeDeTrabalhoImediata unidadeDeTrabalho;
    ProcessarEventoSagaUseCase useCase;
    UUID osId;

    @BeforeEach
    void setUp() {
        unidadeDeTrabalho = new UnidadeDeTrabalhoImediata();
        useCase = new ProcessarEventoSagaUseCase(sagaRepository, osRepository,
            new CompensacaoEstornoEstoque(pecaRepository), montadorDados, publicador, mensagensProcessadas, metricas,
            unidadeDeTrabalho, new ConfiguracaoSaga(PRAZO, 3, 20), Clock.fixed(AGORA, ZoneOffset.UTC));
        osId = UUID.randomUUID();
    }

    @Test
    void mensagemJaProcessadaEhDescartada() {
        var evento = evento(TipoEvento.DIAGNOSTICO_INICIADO);
        when(mensagensProcessadas.registrarSeInedita(evento.mensagemId(), "DiagnosticoIniciado")).thenReturn(false);

        assertThat(useCase.executar(osId, evento)).isEqualTo(ResultadoConsumo.DUPLICADA);

        verifyNoInteractions(sagaRepository, osRepository, publicador);
        assertThat(unidadeDeTrabalho.execucoes()).isEqualTo(1);
    }

    @Test
    void eventoAplicadoAtualizaOrdemESaga() {
        var saga = saga(EstadoSaga.AGUARDANDO_FILA_DIAGNOSTICO);
        var os = ordem(StatusOS.RECEBIDA);
        prepararCarga(saga, os);

        var resultado = useCase.executar(osId, evento(TipoEvento.DIAGNOSTICO_INICIADO));

        assertThat(resultado).isEqualTo(ResultadoConsumo.APLICADA);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        verify(osRepository).salvar(os);
        verify(sagaRepository).salvar(saga);
        verify(metricas).registrarPermanencia(eq(EstadoSaga.AGUARDANDO_FILA_DIAGNOSTICO), any());
        verifyNoInteractions(publicador);
    }

    @Test
    void eventoQueNaoMudaAOrdemNaoGravaAOrdem() {
        var saga = saga(EstadoSaga.AGUARDANDO_FILA_DIAGNOSTICO);
        prepararCarga(saga, ordem(StatusOS.RECEBIDA));

        var resultado = useCase.executar(osId, EventoSaga.builder().mensagemId(UUID.randomUUID())
            .tipo(TipoEvento.EXECUCAO_ENFILEIRADA).etapa(EtapaExecucao.DIAGNOSTICO).ocorridoEm(AGORA).build());

        assertThat(resultado).isEqualTo(ResultadoConsumo.APLICADA);
        verify(osRepository, never()).salvar(any());
        verify(sagaRepository).salvar(saga);
    }

    @Test
    void eventoIgnoradoNaoTemEfeitoNaOrdem() {
        var saga = saga(EstadoSaga.CONCLUIDA);
        prepararCarga(saga, ordem(StatusOS.FINALIZADA));

        var resultado = useCase.executar(osId, evento(TipoEvento.EXECUCAO_FINALIZADA));

        assertThat(resultado).isEqualTo(ResultadoConsumo.IGNORADA);
        verify(osRepository, never()).salvar(any());
        verify(sagaRepository).salvar(saga);
        verifyNoInteractions(metricas);
    }

    @Test
    void diagnosticoConcluidoComItensPublicaPedidoDeOrcamentoComPrazo() {
        var saga = saga(EstadoSaga.EM_DIAGNOSTICO);
        var os = ordem(StatusOS.EM_DIAGNOSTICO);
        os.adicionarServico(new ItemServico(UUID.randomUUID(), UUID.randomUUID(), "Revisão", new BigDecimal("100.00"), 1));
        prepararCarga(saga, os);
        Map<String, Object> dados = Map.of("itens", List.of());
        when(montadorDados.montar(any(), eq(os))).thenReturn(dados);
        var evento = evento(TipoEvento.DIAGNOSTICO_CONCLUIDO);

        useCase.executar(osId, evento);

        var captor = ArgumentCaptor.forClass(ComandoAEnviar.class);
        verify(publicador).publicar(captor.capture());
        var comando = captor.getValue();
        assertThat(comando.tipo()).isEqualTo(TipoComando.GERAR_ORCAMENTO);
        assertThat(comando.sagaId()).isEqualTo(saga.getId());
        assertThat(comando.osId()).isEqualTo(osId);
        assertThat(comando.causadaPor()).isEqualTo(evento.mensagemId());
        assertThat(comando.dados()).isSameAs(dados);
        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.AGUARDANDO_ORCAMENTO);
        assertThat(saga.getPrazoResposta()).isEqualTo(AGORA.plus(PRAZO));
        assertThat(saga.getUltimoComandoTipo()).isEqualTo(TipoComando.GERAR_ORCAMENTO);
    }

    @Test
    void ultimaRespostaDeCompensacaoEstornaEstoqueCancelaOrdemERegistraMetrica() {
        var saga = SagaOrdemServico.reconstituir(DadosSagaOrdemServico.builder()
            .id(UUID.randomUUID()).osId(osId).estado(EstadoSaga.COMPENSANDO)
            .motivoCompensacao(MotivoCompensacao.REPROVACAO)
            .compensacoesPendentes(MotivoCompensacao.REPROVACAO.compensacoes())
            .criadaEm(AGORA).atualizadaEm(AGORA).build());
        var pecaId = UUID.randomUUID();
        var os = OrdemServico.reconstituir(DadosOrdemServico.builder().id(osId).clienteId(UUID.randomUUID())
            .veiculoId(UUID.randomUUID()).status(StatusOS.AGUARDANDO_APROVACAO).dataAbertura(LocalDateTime.now())
            .itensPeca(List.of(new ItemPeca(UUID.randomUUID(), pecaId, "Filtro", new BigDecimal("30.00"), 2)))
            .build());
        var peca = new Peca(pecaId, "Filtro", "", new BigDecimal("30.00"), 5, 1);
        prepararCarga(saga, os);
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));

        var resultado = useCase.executar(osId, evento(TipoEvento.EXECUCAO_CANCELADA));

        assertThat(resultado).isEqualTo(ResultadoConsumo.APLICADA);
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.COMPENSADA);
        verify(osRepository).salvar(os);
        verify(metricas).registrarCompensacao(saga.getId(), MotivoCompensacao.REPROVACAO);
    }

    @Test
    void conclusaoEFalhaAlimentamAsMetricasDaSaga() {
        var concluindo = saga(EstadoSaga.EM_EXECUCAO);
        prepararCarga(concluindo, ordem(StatusOS.EM_EXECUCAO));
        useCase.executar(osId, evento(TipoEvento.EXECUCAO_FINALIZADA));
        verify(metricas).registrarConclusao(concluindo.getId());

        var falhando = saga(EstadoSaga.AGUARDANDO_FILA_REPARO);
        prepararCarga(falhando, ordem(StatusOS.EM_EXECUCAO));
        useCase.executar(osId, EventoSaga.builder().mensagemId(UUID.randomUUID()).tipo(TipoEvento.COMANDO_REJEITADO)
            .comandoRejeitado("IniciarReparo").detalhe("execução inexistente").ocorridoEm(AGORA).build());
        verify(metricas).registrarFalha(eq(falhando.getId()), eq(EstadoSaga.AGUARDANDO_FILA_REPARO), anyString());
    }

    @Test
    void sagaInexistenteGeraExcecaoParaNovaTentativa() {
        when(mensagensProcessadas.registrarSeInedita(any(), anyString())).thenReturn(true);
        when(sagaRepository.buscarPorOsId(osId)).thenReturn(Optional.empty());
        var evento = evento(TipoEvento.DIAGNOSTICO_INICIADO);

        assertThatThrownBy(() -> useCase.executar(osId, evento)).isInstanceOf(SagaNaoEncontradaException.class);
    }

    @Test
    void ordemInexistenteGeraExcecao() {
        when(mensagensProcessadas.registrarSeInedita(any(), anyString())).thenReturn(true);
        when(sagaRepository.buscarPorOsId(osId)).thenReturn(Optional.of(saga(EstadoSaga.AGUARDANDO_DIAGNOSTICO)));
        when(osRepository.buscarPorId(osId)).thenReturn(Optional.empty());
        var evento = evento(TipoEvento.DIAGNOSTICO_INICIADO);

        assertThatThrownBy(() -> useCase.executar(osId, evento)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void eventoForaDeOrdemPropagaSemGravar() {
        prepararCarga(saga(EstadoSaga.AGUARDANDO_APROVACAO), ordem(StatusOS.AGUARDANDO_APROVACAO));
        var evento = evento(TipoEvento.PAGAMENTO_APROVADO);

        assertThatThrownBy(() -> useCase.executar(osId, evento)).isInstanceOf(EventoForaDeOrdemException.class);
        verify(sagaRepository, never()).salvar(any());
        verify(osRepository, never()).salvar(any());
    }

    @Test
    void tipoDesconhecidoFicaRegistradoComoRejeitado() {
        var saga = saga(EstadoSaga.EM_DIAGNOSTICO);
        var mensagemId = UUID.randomUUID();
        when(mensagensProcessadas.registrarSeInedita(mensagemId, "Desconhecido")).thenReturn(true);
        when(sagaRepository.buscarPorOsId(osId)).thenReturn(Optional.of(saga));

        var resultado = useCase.rejeitar(osId, mensagemId, "Desconhecido", "tipo desconhecido");

        assertThat(resultado).isEqualTo(ResultadoConsumo.REJEITADA);
        assertThat(resultado.rotulo()).isEqualTo("rejeitada");
        assertThat(saga.drenarPassos()).singleElement()
            .satisfies(passo -> assertThat(passo.resultado()).isEqualTo(ResultadoPasso.REJEITADO));
        verify(sagaRepository).salvar(saga);
    }

    @Test
    void rejeicaoDuplicadaOuSemSagaNaoGrava() {
        var duplicada = UUID.randomUUID();
        when(mensagensProcessadas.registrarSeInedita(duplicada, "X")).thenReturn(false);
        assertThat(useCase.rejeitar(osId, duplicada, "X", "x")).isEqualTo(ResultadoConsumo.DUPLICADA);

        var semSaga = UUID.randomUUID();
        when(mensagensProcessadas.registrarSeInedita(semSaga, "X")).thenReturn(true);
        when(sagaRepository.buscarPorOsId(osId)).thenReturn(Optional.empty());
        assertThat(useCase.rejeitar(osId, semSaga, "X", "x")).isEqualTo(ResultadoConsumo.REJEITADA);
        verify(sagaRepository, never()).salvar(any());
    }

    private void prepararCarga(SagaOrdemServico saga, OrdemServico os) {
        when(mensagensProcessadas.registrarSeInedita(any(), anyString())).thenReturn(true);
        when(sagaRepository.buscarPorOsId(osId)).thenReturn(Optional.of(saga));
        when(osRepository.buscarPorId(osId)).thenReturn(Optional.of(os));
    }

    private SagaOrdemServico saga(EstadoSaga estado) {
        return SagaOrdemServico.reconstituir(DadosSagaOrdemServico.builder()
            .id(UUID.randomUUID()).osId(osId).estado(estado).criadaEm(AGORA.minusSeconds(60))
            .atualizadaEm(AGORA.minusSeconds(60)).build());
    }

    private OrdemServico ordem(StatusOS status) {
        return OrdemServico.reconstituir(DadosOrdemServico.builder().id(osId).clienteId(UUID.randomUUID())
            .veiculoId(UUID.randomUUID()).status(status).dataAbertura(LocalDateTime.now()).build());
    }

    private static EventoSaga evento(TipoEvento tipo) {
        return EventoSaga.builder().mensagemId(UUID.randomUUID()).tipo(tipo).ocorridoEm(AGORA).build();
    }
}
