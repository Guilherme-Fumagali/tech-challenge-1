package com.oficina.mecanica.domain.saga;

import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.EventoForaDeOrdemException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.oficina.mecanica.domain.saga.EstadoSaga.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class SagaOrdemServicoTest {

    private static final Instant AGORA = Instant.parse("2026-10-30T14:00:00Z");
    private static final Duration PRAZO = Duration.ofMinutes(2);

    @Nested
    @DisplayName("Transições da SPEC-08 §7.3")
    class Transicoes {

        @Test
        void aberturaDaOrdemIniciaASagaAguardandoAFilaDeDiagnostico() {
            var saga = SagaOrdemServico.iniciar(UUID.randomUUID(), UUID.randomUUID(), AGORA);
            var mensagemId = UUID.randomUUID();

            saga.registrarEnvio(mensagemId, TipoComando.INICIAR_DIAGNOSTICO, Map.of("clienteId", "c"), AGORA, PRAZO);

            assertThat(saga.getEstado()).isEqualTo(AGUARDANDO_FILA_DIAGNOSTICO);
            assertThat(saga.getUltimoComandoTipo()).isEqualTo(TipoComando.INICIAR_DIAGNOSTICO);
            assertThat(saga.getUltimoComandoDados()).containsEntry("clienteId", "c");
            assertThat(saga.getPrazoResposta()).isEqualTo(AGORA.plus(PRAZO));
            assertThat(saga.drenarPassos()).singleElement().satisfies(passo -> {
                assertThat(passo.direcao()).isEqualTo(DirecaoPasso.ENVIADO);
                assertThat(passo.tipo()).isEqualTo("IniciarDiagnostico");
                assertThat(passo.mensagemId()).isEqualTo(mensagemId);
            });
        }

        @Test
        void execucaoEnfileiradaParaDiagnosticoPassaAAguardarODiagnostico() {
            var saga = sagaEm(AGUARDANDO_FILA_DIAGNOSTICO);

            var decisao = saga.aplicar(enfileirada(EtapaExecucao.DIAGNOSTICO), false, AGORA);

            assertAplicada(decisao, AGUARDANDO_DIAGNOSTICO, AcaoOrdemServico.NENHUMA);
            assertThat(decisao.comandos()).isEmpty();
            assertThat(saga.getPrazoResposta()).isNull();
        }

        @ParameterizedTest
        @EnumSource(value = EstadoSaga.class, names = {"AGUARDANDO_FILA_DIAGNOSTICO", "AGUARDANDO_DIAGNOSTICO"})
        void diagnosticoIniciadoColocaAOrdemEmDiagnostico(EstadoSaga estado) {
            var saga = sagaEm(estado);

            var decisao = saga.aplicar(evento(TipoEvento.DIAGNOSTICO_INICIADO), false, AGORA);

            assertAplicada(decisao, EM_DIAGNOSTICO, AcaoOrdemServico.REGISTRAR_INICIO_DIAGNOSTICO);
            assertThat(saga.getPrazoResposta()).isNull();
        }

        @Test
        void diagnosticoConcluidoSemItensReabreODiagnostico() {
            var saga = sagaEm(EM_DIAGNOSTICO);

            var decisao = saga.aplicar(evento(TipoEvento.DIAGNOSTICO_CONCLUIDO), false, AGORA);

            assertAplicada(decisao, EM_DIAGNOSTICO, AcaoOrdemServico.NENHUMA);
            assertThat(decisao.mudouDeEstado()).isFalse();
            assertThat(decisao.comandos()).extracting(ComandoSaga::tipo).containsExactly(TipoComando.REABRIR_DIAGNOSTICO);
            assertThat(decisao.comandos().get(0).motivo()).contains("sem serviços ou peças");
        }

        @Test
        void diagnosticoConcluidoComItensPedeOOrcamento() {
            var saga = sagaEm(EM_DIAGNOSTICO);

            var decisao = saga.aplicar(evento(TipoEvento.DIAGNOSTICO_CONCLUIDO), true, AGORA);

            assertAplicada(decisao, AGUARDANDO_ORCAMENTO, AcaoOrdemServico.NENHUMA);
            assertThat(decisao.comandos()).extracting(ComandoSaga::tipo).containsExactly(TipoComando.GERAR_ORCAMENTO);
        }

        @Test
        void orcamentoGeradoAguardaAprovacaoDoCliente() {
            var saga = sagaEm(AGUARDANDO_ORCAMENTO);

            var decisao = saga.aplicar(evento(TipoEvento.ORCAMENTO_GERADO), true, AGORA);

            assertAplicada(decisao, AGUARDANDO_APROVACAO, AcaoOrdemServico.REGISTRAR_ORCAMENTO_ENVIADO);
            assertThat(saga.getPrazoResposta()).isNull();
        }

        @Test
        void rejeicaoDoPedidoDeOrcamentoIniciaCompensacaoPorRejeicao() {
            var saga = sagaEm(AGUARDANDO_ORCAMENTO);

            var decisao = saga.aplicar(rejeicao("GerarOrcamento"), true, AGORA);

            assertAplicada(decisao, COMPENSANDO, AcaoOrdemServico.NENHUMA);
            assertThat(saga.getMotivoCompensacao()).isEqualTo(MotivoCompensacao.REJEICAO);
            assertThat(decisao.comandos()).containsExactly(new ComandoSaga(TipoComando.CANCELAR_EXECUCAO, "REJEICAO"));
        }

        @Test
        void orcamentoAprovadoAguardaPagamento() {
            var saga = sagaEm(AGUARDANDO_APROVACAO);

            var decisao = saga.aplicar(evento(TipoEvento.ORCAMENTO_APROVADO), true, AGORA);

            assertAplicada(decisao, AGUARDANDO_PAGAMENTO, AcaoOrdemServico.REGISTRAR_APROVACAO);
        }

        @ParameterizedTest
        @EnumSource(value = TipoEvento.class, names = {"ORCAMENTO_REPROVADO", "ORCAMENTO_EXPIRADO"})
        void orcamentoReprovadoOuExpiradoIniciaCompensacaoPorReprovacao(TipoEvento tipo) {
            var saga = sagaEm(AGUARDANDO_APROVACAO);

            var decisao = saga.aplicar(evento(tipo), true, AGORA);

            assertAplicada(decisao, COMPENSANDO, AcaoOrdemServico.NENHUMA);
            assertThat(saga.getMotivoCompensacao()).isEqualTo(MotivoCompensacao.REPROVACAO);
            assertThat(decisao.comandos()).containsExactly(new ComandoSaga(TipoComando.CANCELAR_EXECUCAO, "REPROVACAO"));
        }

        @Test
        void pagamentoAprovadoPoeAOrdemEmExecucaoEPedeOReparo() {
            var saga = sagaEm(AGUARDANDO_PAGAMENTO);

            var decisao = saga.aplicar(evento(TipoEvento.PAGAMENTO_APROVADO), true, AGORA);

            assertAplicada(decisao, AGUARDANDO_FILA_REPARO, AcaoOrdemServico.REGISTRAR_PAGAMENTO);
            assertThat(decisao.comandos()).extracting(ComandoSaga::tipo).containsExactly(TipoComando.INICIAR_REPARO);
        }

        @Test
        void pagamentoRecusadoIniciaCompensacaoPorPagamentoRecusado() {
            var saga = sagaEm(AGUARDANDO_PAGAMENTO);

            var decisao = saga.aplicar(evento(TipoEvento.PAGAMENTO_RECUSADO), true, AGORA);

            assertAplicada(decisao, COMPENSANDO, AcaoOrdemServico.NENHUMA);
            assertThat(saga.getMotivoCompensacao()).isEqualTo(MotivoCompensacao.PAGAMENTO_RECUSADO);
            assertThat(decisao.comandos())
                .containsExactly(new ComandoSaga(TipoComando.CANCELAR_ORCAMENTO, "PAGAMENTO_RECUSADO"));
        }

        @Test
        void execucaoEnfileiradaParaReparoPassaAEmExecucao() {
            var saga = sagaEm(AGUARDANDO_FILA_REPARO);

            var decisao = saga.aplicar(enfileirada(EtapaExecucao.REPARO), true, AGORA);

            assertAplicada(decisao, EM_EXECUCAO, AcaoOrdemServico.NENHUMA);
        }

        @ParameterizedTest
        @EnumSource(value = EstadoSaga.class, names = {"AGUARDANDO_FILA_REPARO", "EM_EXECUCAO"})
        void reparoIniciadoRegistraNoHistorico(EstadoSaga estado) {
            var saga = sagaEm(estado);

            var decisao = saga.aplicar(evento(TipoEvento.REPARO_INICIADO), true, AGORA);

            assertAplicada(decisao, EM_EXECUCAO, AcaoOrdemServico.REGISTRAR_INICIO_REPARO);
        }

        @Test
        void execucaoFinalizadaConcluiASaga() {
            var saga = sagaEm(EM_EXECUCAO);

            var decisao = saga.aplicar(evento(TipoEvento.EXECUCAO_FINALIZADA), true, AGORA);

            assertAplicada(decisao, CONCLUIDA, AcaoOrdemServico.REGISTRAR_CONCLUSAO);
            assertThat(saga.getEstado().encerrado()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = EstadoSaga.class, names = {"AGUARDANDO_FILA_REPARO", "EM_EXECUCAO"})
        void falhaNaExecucaoIniciaCompensacaoPorFalha(EstadoSaga estado) {
            var saga = sagaEm(estado);

            var decisao = saga.aplicar(evento(TipoEvento.EXECUCAO_FALHOU), true, AGORA);

            assertAplicada(decisao, COMPENSANDO, AcaoOrdemServico.NENHUMA);
            assertThat(saga.getMotivoCompensacao()).isEqualTo(MotivoCompensacao.FALHA_EXECUCAO);
            assertThat(decisao.comandos())
                .containsExactly(new ComandoSaga(TipoComando.ESTORNAR_PAGAMENTO, "FALHA_EXECUCAO"));
        }

        @Test
        void respostaDaCompensacaoCorrenteLiberaAProxima() {
            var saga = sagaCompensando(MotivoCompensacao.PAGAMENTO_RECUSADO);

            var decisao = saga.aplicar(evento(TipoEvento.ORCAMENTO_CANCELADO), true, AGORA);

            assertAplicada(decisao, COMPENSANDO, AcaoOrdemServico.NENHUMA);
            assertThat(decisao.comandos())
                .containsExactly(new ComandoSaga(TipoComando.CANCELAR_EXECUCAO, "PAGAMENTO_RECUSADO"));
            assertThat(saga.getCompensacoesPendentes())
                .containsExactly(Compensacao.CANCELAR_EXECUCAO, Compensacao.ESTORNAR_ESTOQUE);
        }

        @Test
        void respostaDaUltimaCompensacaoRemotaExecutaOEstornoLocalEEncerra() {
            var saga = sagaCompensando(MotivoCompensacao.REPROVACAO);

            var decisao = saga.aplicar(evento(TipoEvento.EXECUCAO_CANCELADA), true, AGORA);

            assertAplicada(decisao, COMPENSADA, AcaoOrdemServico.NENHUMA);
            assertThat(decisao.compensacaoLocal()).isEqualTo(Compensacao.ESTORNAR_ESTOQUE);
            assertThat(decisao.executaCompensacaoLocal()).isTrue();
            assertThat(decisao.comandos()).isEmpty();
            assertThat(saga.getCompensacoesPendentes()).isEmpty();
            assertThat(saga.getPrazoResposta()).isNull();
        }
    }

    @Nested
    @DisplayName("Ordem das compensações (SPEC-08 §7.4)")
    class OrdemDasCompensacoes {

        @Test
        void reprovacaoCancelaAExecucaoEDepoisEstornaOEstoque() {
            var saga = sagaEm(AGUARDANDO_APROVACAO);

            var sequencia = new java.util.ArrayList<Object>();
            sequencia.addAll(comandos(saga.aplicar(evento(TipoEvento.ORCAMENTO_REPROVADO), true, AGORA)));
            sequencia.add(saga.aplicar(evento(TipoEvento.EXECUCAO_CANCELADA), true, AGORA).compensacaoLocal());

            assertThat(sequencia).containsExactly(TipoComando.CANCELAR_EXECUCAO, Compensacao.ESTORNAR_ESTOQUE);
            assertThat(saga.getEstado()).isEqualTo(COMPENSADA);
        }

        @Test
        void rejeicaoCancelaAExecucaoEDepoisEstornaOEstoque() {
            var saga = sagaEm(AGUARDANDO_ORCAMENTO);

            var sequencia = new java.util.ArrayList<Object>();
            sequencia.addAll(comandos(saga.aplicar(rejeicao("GerarOrcamento"), true, AGORA)));
            sequencia.add(saga.aplicar(evento(TipoEvento.EXECUCAO_CANCELADA), true, AGORA).compensacaoLocal());

            assertThat(sequencia).containsExactly(TipoComando.CANCELAR_EXECUCAO, Compensacao.ESTORNAR_ESTOQUE);
            assertThat(saga.getEstado()).isEqualTo(COMPENSADA);
        }

        @Test
        void pagamentoRecusadoCancelaOrcamentoExecucaoEEstornaOEstoque() {
            var saga = sagaEm(AGUARDANDO_PAGAMENTO);

            var sequencia = new java.util.ArrayList<Object>();
            sequencia.addAll(comandos(saga.aplicar(evento(TipoEvento.PAGAMENTO_RECUSADO), true, AGORA)));
            sequencia.addAll(comandos(saga.aplicar(evento(TipoEvento.ORCAMENTO_CANCELADO), true, AGORA)));
            sequencia.add(saga.aplicar(evento(TipoEvento.EXECUCAO_CANCELADA), true, AGORA).compensacaoLocal());

            assertThat(sequencia).containsExactly(TipoComando.CANCELAR_ORCAMENTO, TipoComando.CANCELAR_EXECUCAO,
                Compensacao.ESTORNAR_ESTOQUE);
            assertThat(saga.getEstado()).isEqualTo(COMPENSADA);
        }

        @Test
        void falhaNaExecucaoEstornaPagamentoCancelaOrcamentoEEstornaOEstoque() {
            var saga = sagaEm(EM_EXECUCAO);

            var sequencia = new java.util.ArrayList<Object>();
            sequencia.addAll(comandos(saga.aplicar(evento(TipoEvento.EXECUCAO_FALHOU), true, AGORA)));
            sequencia.addAll(comandos(saga.aplicar(evento(TipoEvento.PAGAMENTO_ESTORNADO), true, AGORA)));
            sequencia.add(saga.aplicar(evento(TipoEvento.ORCAMENTO_CANCELADO), true, AGORA).compensacaoLocal());

            assertThat(sequencia).containsExactly(TipoComando.ESTORNAR_PAGAMENTO, TipoComando.CANCELAR_ORCAMENTO,
                Compensacao.ESTORNAR_ESTOQUE);
            assertThat(saga.getEstado()).isEqualTo(COMPENSADA);
        }

        @Test
        void estornoLocalFicaRegistradoComoPassoLocal() {
            var saga = sagaCompensando(MotivoCompensacao.REJEICAO);
            saga.drenarPassos();

            saga.aplicar(evento(TipoEvento.EXECUCAO_CANCELADA), true, AGORA);

            assertThat(saga.drenarPassos()).extracting(PassoSaga::direcao, PassoSaga::tipo, PassoSaga::detalhe)
                .containsExactly(
                    tuple(DirecaoPasso.RECEBIDO, "ExecucaoCancelada", null),
                    tuple(DirecaoPasso.LOCAL, "ESTORNAR_ESTOQUE", "REJEICAO"));
        }

        @Test
        void rejeicaoDoComandoDeCompensacaoLevaASagaParaFalha() {
            var saga = sagaCompensando(MotivoCompensacao.PAGAMENTO_RECUSADO);

            var decisao = saga.aplicar(rejeicao("CancelarOrcamento"), true, AGORA);

            assertAplicada(decisao, FALHA, AcaoOrdemServico.NENHUMA);
            assertThat(decisao.detalhe()).contains("CANCELAR_ORCAMENTO");
            assertThat(decisao.comandos()).isEmpty();
        }

        @Test
        void respostaQueNaoEhDaCompensacaoCorrenteEhIgnorada() {
            var saga = sagaCompensando(MotivoCompensacao.PAGAMENTO_RECUSADO);

            var decisao = saga.aplicar(evento(TipoEvento.EXECUCAO_CANCELADA), true, AGORA);

            assertIgnorada(decisao, COMPENSANDO);
            assertThat(decisao.detalhe()).contains("OrcamentoCancelado");
            assertThat(saga.getCompensacoesPendentes()).hasSize(3);
        }

        @Test
        void rejeicaoDeOutroComandoDuranteACompensacaoEhIgnorada() {
            var saga = sagaCompensando(MotivoCompensacao.REPROVACAO);

            assertIgnorada(saga.aplicar(rejeicao("GerarOrcamento"), true, AGORA), COMPENSANDO);
        }

        @Test
        void compensandoSemPendenciasIgnoraEventos() {
            var saga = SagaOrdemServico.reconstituir(DadosSagaOrdemServico.builder()
                .id(UUID.randomUUID()).osId(UUID.randomUUID()).estado(COMPENSANDO)
                .motivoCompensacao(MotivoCompensacao.REPROVACAO).criadaEm(AGORA).build());

            assertIgnorada(saga.aplicar(evento(TipoEvento.EXECUCAO_CANCELADA), true, AGORA), COMPENSANDO);
        }
    }

    @Nested
    @DisplayName("Eventos ignorados")
    class EventosIgnorados {

        @Test
        void enfileiramentoDoDiagnosticoDepoisDoInicioJaFoiSuperado() {
            var saga = sagaEm(EM_DIAGNOSTICO);

            assertIgnorada(saga.aplicar(enfileirada(EtapaExecucao.DIAGNOSTICO), true, AGORA), EM_DIAGNOSTICO);
        }

        @Test
        void aprovacaoRepetidaComOutraMensagemEhIgnorada() {
            var saga = sagaEm(AGUARDANDO_PAGAMENTO);

            var decisao = saga.aplicar(evento(TipoEvento.ORCAMENTO_APROVADO), true, AGORA);

            assertIgnorada(decisao, AGUARDANDO_PAGAMENTO);
            assertThat(decisao.detalhe()).contains("já refletido");
        }

        @ParameterizedTest
        @EnumSource(value = EstadoSaga.class, names = {"CONCLUIDA", "COMPENSADA", "FALHA"})
        void sagaEncerradaIgnoraQualquerEvento(EstadoSaga estado) {
            var saga = sagaEm(estado);

            assertIgnorada(saga.aplicar(evento(TipoEvento.EXECUCAO_FINALIZADA), true, AGORA), estado);
        }

        @ParameterizedTest
        @EnumSource(value = TipoEvento.class,
            names = {"EXECUCAO_CANCELADA", "ORCAMENTO_CANCELADO", "PAGAMENTO_ESTORNADO"})
        void respostaDeCompensacaoForaDaCompensacaoEhIgnorada(TipoEvento tipo) {
            var saga = sagaEm(AGUARDANDO_APROVACAO);

            assertIgnorada(saga.aplicar(evento(tipo), true, AGORA), AGUARDANDO_APROVACAO);
        }

        @Test
        void enfileiramentoSemEtapaEhIgnorado() {
            var saga = sagaEm(AGUARDANDO_FILA_DIAGNOSTICO);

            assertIgnorada(saga.aplicar(evento(TipoEvento.EXECUCAO_ENFILEIRADA), true, AGORA),
                AGUARDANDO_FILA_DIAGNOSTICO);
        }

        @Test
        void rejeicaoDeComandoQueASagaNaoAguardaEhIgnorada() {
            var saga = sagaEm(AGUARDANDO_ORCAMENTO);

            assertIgnorada(saga.aplicar(rejeicao("ReabrirDiagnostico"), true, AGORA), AGUARDANDO_ORCAMENTO);
            assertIgnorada(saga.aplicar(rejeicao(null), true, AGORA), AGUARDANDO_ORCAMENTO);
        }

        @Test
        void rejeicaoNumEstadoQueNaoAguardaComandoEhIgnorada() {
            var saga = sagaEm(AGUARDANDO_APROVACAO);

            assertIgnorada(saga.aplicar(rejeicao("GerarOrcamento"), true, AGORA), AGUARDANDO_APROVACAO);
        }

        @Test
        void eventoIgnoradoFicaNoLogDaSagaSemAlterarAData() {
            var saga = sagaEm(CONCLUIDA);
            var mensagemId = UUID.randomUUID();

            saga.aplicar(EventoSaga.builder().mensagemId(mensagemId).tipo(TipoEvento.PAGAMENTO_APROVADO)
                .ocorridoEm(AGORA).build(), true, AGORA.plusSeconds(60));

            assertThat(saga.getAtualizadaEm()).isEqualTo(AGORA);
            assertThat(saga.drenarPassos()).singleElement().satisfies(passo -> {
                assertThat(passo.resultado()).isEqualTo(ResultadoPasso.IGNORADO);
                assertThat(passo.mensagemId()).isEqualTo(mensagemId);
            });
        }
    }

    @Nested
    @DisplayName("Eventos fora de ordem")
    class EventosForaDeOrdem {

        @Test
        void pagamentoAprovadoAntesDaAprovacaoDoOrcamento() {
            var saga = sagaEm(AGUARDANDO_APROVACAO);
            var evento = evento(TipoEvento.PAGAMENTO_APROVADO);

            assertThatThrownBy(() -> saga.aplicar(evento, true, AGORA))
                .isInstanceOf(EventoForaDeOrdemException.class)
                .hasMessageContaining("PagamentoAprovado")
                .hasMessageContaining("AGUARDANDO_APROVACAO");
            assertThat(saga.getEstado()).isEqualTo(AGUARDANDO_APROVACAO);
            assertThat(saga.drenarPassos()).isEmpty();
        }

        @Test
        void execucaoFinalizadaAntesDoEnfileiramentoDoReparo() {
            var saga = sagaEm(AGUARDANDO_FILA_REPARO);
            var evento = evento(TipoEvento.EXECUCAO_FINALIZADA);

            assertThatThrownBy(() -> saga.aplicar(evento, true, AGORA))
                .isInstanceOf(EventoForaDeOrdemException.class);
        }

        @Test
        void diagnosticoConcluidoAntesDoInicio() {
            var saga = sagaEm(AGUARDANDO_DIAGNOSTICO);
            var evento = evento(TipoEvento.DIAGNOSTICO_CONCLUIDO);

            assertThatThrownBy(() -> saga.aplicar(evento, true, AGORA))
                .isInstanceOf(EventoForaDeOrdemException.class);
        }

        @Test
        void orcamentoGeradoAntesDoPedidoDeOrcamento() {
            var saga = sagaEm(EM_DIAGNOSTICO);
            var evento = evento(TipoEvento.ORCAMENTO_GERADO);

            assertThatThrownBy(() -> saga.aplicar(evento, true, AGORA))
                .isInstanceOf(EventoForaDeOrdemException.class);
        }

        @Test
        void enfileiramentoDoReparoAntesDoPagamento() {
            var saga = sagaEm(AGUARDANDO_PAGAMENTO);
            var evento = enfileirada(EtapaExecucao.REPARO);

            assertThatThrownBy(() -> saga.aplicar(evento, true, AGORA))
                .isInstanceOf(EventoForaDeOrdemException.class);
        }
    }

    @Nested
    @DisplayName("Rejeições sem compensação definida")
    class RejeicoesSemCompensacao {

        @Test
        void rejeicaoDoPedidoDeDiagnosticoLevaParaFalha() {
            var saga = sagaEm(AGUARDANDO_FILA_DIAGNOSTICO);

            var decisao = saga.aplicar(rejeicao("IniciarDiagnostico"), false, AGORA);

            assertAplicada(decisao, FALHA, AcaoOrdemServico.NENHUMA);
            assertThat(decisao.detalhe()).contains("IniciarDiagnostico rejeitado");
        }

        @Test
        void rejeicaoDoPedidoDeReparoLevaParaFalha() {
            var saga = sagaEm(AGUARDANDO_FILA_REPARO);

            assertAplicada(saga.aplicar(rejeicao("IniciarReparo"), true, AGORA), FALHA, AcaoOrdemServico.NENHUMA);
        }
    }

    @Nested
    @DisplayName("Prazos e reenvio (SPEC-08 §7.5)")
    class Prazos {

        @Test
        void comandoSemRespostaEsperadaNaoAbrePrazo() {
            var saga = sagaEm(EM_DIAGNOSTICO);

            saga.registrarEnvio(UUID.randomUUID(), TipoComando.REABRIR_DIAGNOSTICO, Map.of(), AGORA, PRAZO);

            assertThat(saga.getPrazoResposta()).isNull();
            assertThat(saga.getUltimoComandoTipo()).isNull();
            assertThat(saga.getUltimoComandoDados()).isEmpty();
        }

        @Test
        void prazoVencidoPermiteReenvioComNovaMensagem() {
            var saga = sagaEm(AGUARDANDO_ORCAMENTO);
            saga.registrarEnvio(UUID.randomUUID(), TipoComando.GERAR_ORCAMENTO, Map.of("itens", List.of()), AGORA, PRAZO);
            saga.drenarPassos();
            var depoisDoPrazo = AGORA.plus(PRAZO).plusSeconds(1);
            var novaMensagem = UUID.randomUUID();

            assertThat(saga.prazoVencido(AGORA)).isFalse();
            assertThat(saga.prazoVencido(depoisDoPrazo)).isTrue();
            saga.registrarReenvio(novaMensagem, depoisDoPrazo, PRAZO);

            assertThat(saga.getTentativasReenvio()).isEqualTo(1);
            assertThat(saga.getPrazoResposta()).isEqualTo(depoisDoPrazo.plus(PRAZO));
            assertThat(saga.drenarPassos()).singleElement().satisfies(passo -> {
                assertThat(passo.tipo()).isEqualTo("GerarOrcamento");
                assertThat(passo.mensagemId()).isEqualTo(novaMensagem);
                assertThat(passo.detalhe()).isEqualTo("reenvio 1");
            });
        }

        @Test
        void reenviosEsgotadosLevamAFalha() {
            var saga = sagaEm(AGUARDANDO_FILA_REPARO);
            saga.registrarEnvio(UUID.randomUUID(), TipoComando.INICIAR_REPARO, Map.of(), AGORA, PRAZO);
            for (int i = 0; i < 3; i++) {
                saga.registrarReenvio(UUID.randomUUID(), AGORA, PRAZO);
            }

            assertThat(saga.reenviosEsgotados(3)).isTrue();
            var estadoAnterior = saga.registrarFalhaPorPrazo(AGORA);

            assertThat(estadoAnterior).isEqualTo(AGUARDANDO_FILA_REPARO);
            assertThat(saga.getEstado()).isEqualTo(FALHA);
            assertThat(saga.getPrazoResposta()).isNull();
            assertThat(saga.drenarPassos()).last().satisfies(passo -> {
                assertThat(passo.direcao()).isEqualTo(DirecaoPasso.LOCAL);
                assertThat(passo.tipo()).isEqualTo("PrazoEsgotado");
                assertThat(passo.detalhe()).contains("IniciarReparo").contains("3 reenvios");
            });
        }

        @Test
        void novoComandoZeraAsTentativas() {
            var saga = sagaEm(AGUARDANDO_FILA_DIAGNOSTICO);
            saga.registrarEnvio(UUID.randomUUID(), TipoComando.INICIAR_DIAGNOSTICO, Map.of(), AGORA, PRAZO);
            saga.registrarReenvio(UUID.randomUUID(), AGORA, PRAZO);

            saga.registrarEnvio(UUID.randomUUID(), TipoComando.GERAR_ORCAMENTO, Map.of(), AGORA, PRAZO);

            assertThat(saga.getTentativasReenvio()).isZero();
            assertThat(saga.reenviosEsgotados(1)).isFalse();
        }

        @Test
        void reenvioSemComandoPendenteEhRecusado() {
            var saga = sagaEm(AGUARDANDO_APROVACAO);
            var mensagem = UUID.randomUUID();

            assertThatThrownBy(() -> saga.registrarReenvio(mensagem, AGORA, PRAZO))
                .isInstanceOf(DomainException.class);
        }

        @Test
        void transicaoParaEstadoQueEsperaPessoaEncerraOPrazo() {
            var saga = sagaEm(AGUARDANDO_FILA_DIAGNOSTICO);
            saga.registrarEnvio(UUID.randomUUID(), TipoComando.INICIAR_DIAGNOSTICO, Map.of(), AGORA, PRAZO);

            saga.aplicar(evento(TipoEvento.DIAGNOSTICO_INICIADO), false, AGORA.plusSeconds(5));

            assertThat(saga.getPrazoResposta()).isNull();
            assertThat(saga.prazoVencido(AGORA.plus(Duration.ofHours(1)))).isFalse();
        }

        @Test
        void mudancaDeEstadoInformaAPermanenciaNoEstadoAnterior() {
            var saga = sagaEm(AGUARDANDO_APROVACAO);

            var decisao = saga.aplicar(evento(TipoEvento.ORCAMENTO_APROVADO), true, AGORA.plusSeconds(90));

            assertThat(decisao.permanenciaNoEstadoAnterior()).isEqualTo(Duration.ofSeconds(90));
            assertThat(saga.getAtualizadaEm()).isEqualTo(AGORA.plusSeconds(90));
        }
    }

    @Nested
    @DisplayName("Reconstituição e log")
    class Reconstituicao {

        @Test
        void reconstituirPreservaOEstadoPersistido() {
            var id = UUID.randomUUID();
            var osId = UUID.randomUUID();
            var saga = SagaOrdemServico.reconstituir(DadosSagaOrdemServico.builder()
                .id(id).osId(osId).estado(COMPENSANDO).motivoCompensacao(MotivoCompensacao.FALHA_EXECUCAO)
                .compensacoesPendentes(List.of(Compensacao.CANCELAR_ORCAMENTO, Compensacao.ESTORNAR_ESTOQUE))
                .ultimoComandoTipo(TipoComando.CANCELAR_ORCAMENTO).ultimoComandoDados(Map.of("motivo", "FALHA_EXECUCAO"))
                .prazoResposta(AGORA).tentativasReenvio(2).criadaEm(AGORA.minusSeconds(600)).atualizadaEm(AGORA)
                .versao(7L).build());

            assertThat(saga.getId()).isEqualTo(id);
            assertThat(saga.getOsId()).isEqualTo(osId);
            assertThat(saga.getCompensacoesPendentes())
                .containsExactly(Compensacao.CANCELAR_ORCAMENTO, Compensacao.ESTORNAR_ESTOQUE);
            assertThat(saga.getUltimoComandoDados()).containsEntry("motivo", "FALHA_EXECUCAO");
            assertThat(saga.getTentativasReenvio()).isEqualTo(2);
            assertThat(saga.getVersao()).isEqualTo(7L);
            saga.registrarVersaoPersistida(8L);
            assertThat(saga.getVersao()).isEqualTo(8L);
        }

        @Test
        void eventoRejeitadoFicaNoLog() {
            var saga = sagaEm(EM_DIAGNOSTICO);
            var mensagemId = UUID.randomUUID();

            saga.registrarEventoRejeitado("EventoDesconhecido", mensagemId, "tipo desconhecido", AGORA);

            assertThat(saga.drenarPassos()).singleElement().satisfies(passo -> {
                assertThat(passo.resultado()).isEqualTo(ResultadoPasso.REJEITADO);
                assertThat(passo.tipo()).isEqualTo("EventoDesconhecido");
            });
        }

        @Test
        void catalogosResolvemNomesDasMensagens() {
            assertThat(TipoEvento.porNome("PagamentoAprovado")).contains(TipoEvento.PAGAMENTO_APROVADO);
            assertThat(TipoEvento.porNome("Inexistente")).isEmpty();
            assertThat(TipoComando.porNome("GerarOrcamento")).isEqualTo(TipoComando.GERAR_ORCAMENTO);
            assertThat(TipoComando.GERAR_ORCAMENTO.destino()).isEqualTo(Participante.BILLING);
            assertThat(TipoComando.REABRIR_DIAGNOSTICO.exigeResposta()).isFalse();
            assertThat(Compensacao.ESTORNAR_ESTOQUE.local()).isTrue();
            assertThatThrownBy(() -> TipoComando.porNome("Inexistente")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void instanteDoFatoUsaAOcorrenciaQuandoAusente() {
            var evento = EventoSaga.builder().tipo(TipoEvento.ORCAMENTO_APROVADO).ocorridoEm(AGORA).build();
            var comInstante = EventoSaga.builder().tipo(TipoEvento.ORCAMENTO_APROVADO).ocorridoEm(AGORA)
                .instanteDoFato(AGORA.minusSeconds(5)).build();

            assertThat(evento.instanteDoFatoOuOcorrencia()).isEqualTo(AGORA);
            assertThat(comInstante.instanteDoFatoOuOcorrencia()).isEqualTo(AGORA.minusSeconds(5));
        }
    }

    private static SagaOrdemServico sagaEm(EstadoSaga estado) {
        return SagaOrdemServico.reconstituir(DadosSagaOrdemServico.builder()
            .id(UUID.randomUUID()).osId(UUID.randomUUID()).estado(estado)
            .criadaEm(AGORA).atualizadaEm(AGORA).versao(0L).build());
    }

    private static SagaOrdemServico sagaCompensando(MotivoCompensacao motivo) {
        return SagaOrdemServico.reconstituir(DadosSagaOrdemServico.builder()
            .id(UUID.randomUUID()).osId(UUID.randomUUID()).estado(COMPENSANDO).motivoCompensacao(motivo)
            .compensacoesPendentes(motivo.compensacoes())
            .criadaEm(AGORA).atualizadaEm(AGORA).versao(0L).build());
    }

    private static EventoSaga evento(TipoEvento tipo) {
        return EventoSaga.builder().mensagemId(UUID.randomUUID()).tipo(tipo).ocorridoEm(AGORA).build();
    }

    private static EventoSaga enfileirada(EtapaExecucao etapa) {
        return EventoSaga.builder().mensagemId(UUID.randomUUID()).tipo(TipoEvento.EXECUCAO_ENFILEIRADA)
            .etapa(etapa).ocorridoEm(AGORA).build();
    }

    private static EventoSaga rejeicao(String comando) {
        return EventoSaga.builder().mensagemId(UUID.randomUUID()).tipo(TipoEvento.COMANDO_REJEITADO)
            .comandoRejeitado(comando).detalhe("dados inválidos").ocorridoEm(AGORA).build();
    }

    private static List<TipoComando> comandos(DecisaoSaga decisao) {
        return decisao.comandos().stream().map(ComandoSaga::tipo).toList();
    }

    private static void assertAplicada(DecisaoSaga decisao, EstadoSaga estado, AcaoOrdemServico acao) {
        assertThat(decisao.resultado()).isEqualTo(ResultadoPasso.APLICADO);
        assertThat(decisao.aplicada()).isTrue();
        assertThat(decisao.estadoAtual()).isEqualTo(estado);
        assertThat(decisao.acaoOrdemServico()).isEqualTo(acao);
    }

    private static void assertIgnorada(DecisaoSaga decisao, EstadoSaga estado) {
        assertThat(decisao.resultado()).isEqualTo(ResultadoPasso.IGNORADO);
        assertThat(decisao.aplicada()).isFalse();
        assertThat(decisao.estadoAtual()).isEqualTo(estado);
        assertThat(decisao.acaoOrdemServico()).isEqualTo(AcaoOrdemServico.NENHUMA);
        assertThat(decisao.comandos()).isEmpty();
        assertThat(decisao.executaCompensacaoLocal()).isFalse();
    }
}
