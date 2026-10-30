package com.oficina.mecanica.infrastructure;

import com.oficina.mecanica.DocumentosDeTeste;
import com.oficina.mecanica.application.port.ComandoAEnviar;
import com.oficina.mecanica.application.port.PublicadorMensagens;
import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;
import com.oficina.mecanica.domain.repository.SagaOrdemServicoRepository;
import com.oficina.mecanica.domain.repository.VeiculoRepository;
import com.oficina.mecanica.domain.saga.DadosSagaOrdemServico;
import com.oficina.mecanica.domain.saga.EstadoSaga;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;
import com.oficina.mecanica.domain.saga.TipoComando;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.domain.valueobject.Placa;
import com.oficina.mecanica.infrastructure.messaging.MonitorDlq;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class MensageriaIntegrationTest extends IntegracaoBase {

    @Autowired UnidadeDeTrabalho unidadeDeTrabalho;
    @Autowired PublicadorMensagens publicador;
    @Autowired OrdemServicoRepository osRepository;
    @Autowired SagaOrdemServicoRepository sagaRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired VeiculoRepository veiculoRepository;
    @Autowired PecaRepository pecaRepository;
    @Autowired MonitorDlq monitorDlq;
    @Autowired MeterRegistry registry;
    @Autowired JdbcTemplate jdbc;

    @Test
    void relayPublicaOComandoGravadoNoOutboxEMarcaComoPublicado() {
        var mensagemId = UUID.randomUUID();
        var sagaId = UUID.randomUUID();
        var osId = UUID.randomUUID();

        unidadeDeTrabalho.executar(() -> publicador.publicar(new ComandoAEnviar(mensagemId,
            TipoComando.CANCELAR_ORCAMENTO, sagaId, osId, null, Map.of("motivo", "REPROVACAO"))));

        var recebida = FilasDeTeste.aguardarMensagem(FilasDeTeste.BILLING_COMANDOS,
            m -> m.mensagemId().equals(mensagemId), Duration.ofSeconds(15));
        var envelope = recebida.envelope();
        assertThat(envelope.path("tipo").asText()).isEqualTo("CancelarOrcamento");
        assertThat(envelope.path("versao").asInt()).isEqualTo(1);
        assertThat(envelope.path("sagaId").asText()).isEqualTo(sagaId.toString());
        assertThat(envelope.path("osId").asText()).isEqualTo(osId.toString());
        assertThat(envelope.has("causadaPor")).isTrue();
        assertThat(envelope.path("causadaPor").isNull()).isTrue();
        assertThat(envelope.path("origem").asText()).isEqualTo("os-service");
        assertThat(envelope.path("ocorridoEm").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z");
        assertThat(envelope.path("dados").path("motivo").asText()).isEqualTo("REPROVACAO");
        assertThat(recebida.atributos()).containsKeys("tipo", "versao").doesNotContainKey("JavaType");
        assertThat(recebida.atributos().get("tipo").stringValue()).isEqualTo("CancelarOrcamento");
        assertThat(recebida.atributos().get("versao").dataType()).isEqualTo("String");
        assertThat(recebida.atributos().get("versao").stringValue()).isEqualTo("1");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(jdbc.queryForObject(
            "SELECT publicada_em IS NOT NULL FROM outbox WHERE id = ?", Boolean.class, mensagemId)).isTrue());
        assertThat(registry.counter("oficina.mensageria.publicadas", "tipo", "CancelarOrcamento").count())
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    void eventoRecebidoAvancaASagaEFicaRegistradoNaInbox() {
        var cenario = cenario(EstadoSaga.AGUARDANDO_FILA_DIAGNOSTICO, false);

        var mensagemId = FilasDeTeste.publicarEvento("ExecucaoEnfileirada", cenario.sagaId(), cenario.osId(), null,
            Map.of("etapa", "DIAGNOSTICO", "enfileiradaEm", Instant.now().toString()));

        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> assertThat(estadoSaga(cenario.osId())).isEqualTo("AGUARDANDO_DIAGNOSTICO"));
        assertThat(jdbc.queryForObject("SELECT tipo FROM mensagens_processadas WHERE mensagem_id = ?",
            String.class, mensagemId)).isEqualTo("ExecucaoEnfileirada");
        assertThat(jdbc.queryForObject("SELECT resultado FROM saga_passos WHERE mensagem_id = ?",
            String.class, mensagemId)).isEqualTo("APLICADO");
    }

    @Test
    void mensagemDuplicadaEhDescartadaSemEfeitoRepetido() {
        var cenario = cenario(EstadoSaga.AGUARDANDO_DIAGNOSTICO, false);
        var mensagemId = UUID.randomUUID();
        var duplicadasAntes = registry.counter("oficina.mensageria.consumidas",
            "tipo", "DiagnosticoIniciado", "resultado", "duplicada").count();

        FilasDeTeste.publicarEvento(mensagemId, "DiagnosticoIniciado", cenario.sagaId(), cenario.osId(), null,
            Map.of("mecanicoId", UUID.randomUUID().toString(), "iniciadoEm", Instant.now().toString()));
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> assertThat(statusOs(cenario.osId())).isEqualTo("EM_DIAGNOSTICO"));
        FilasDeTeste.publicarEvento(mensagemId, "DiagnosticoIniciado", cenario.sagaId(), cenario.osId(), null,
            Map.of("mecanicoId", UUID.randomUUID().toString(), "iniciadoEm", Instant.now().toString()));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(registry.counter(
            "oficina.mensageria.consumidas", "tipo", "DiagnosticoIniciado", "resultado", "duplicada").count())
            .isGreaterThan(duplicadasAntes));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saga_passos WHERE mensagem_id = ?",
            Integer.class, mensagemId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM historico_status_os WHERE os_id = ? AND status_destino = 'EM_DIAGNOSTICO'",
            Integer.class, cenario.osId())).isEqualTo(1);
    }

    @Test
    void eventoForaDeOrdemVoltaParaAFilaEAposCincoTentativasVaiParaADlq() {
        var cenario = cenario(EstadoSaga.AGUARDANDO_FILA_DIAGNOSTICO, false);
        FilasDeTeste.esvaziar(FilasDeTeste.DLQ_OS_EVENTOS);

        var mensagemId = FilasDeTeste.publicarEvento("PagamentoAprovado", cenario.sagaId(), cenario.osId(), null,
            Map.of("orcamentoId", UUID.randomUUID().toString(), "valor", 210.00));

        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> assertThat(FilasDeTeste.mensagensAproximadas(FilasDeTeste.DLQ_OS_EVENTOS))
                .isEqualTo(1));
        assertThat(estadoSaga(cenario.osId())).isEqualTo("AGUARDANDO_FILA_DIAGNOSTICO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mensagens_processadas WHERE mensagem_id = ?",
            Integer.class, mensagemId)).isZero();
        assertThat(registry.counter("oficina.mensageria.consumidas", "tipo", "PagamentoAprovado", "resultado", "erro")
            .count()).isGreaterThanOrEqualTo(5);

        monitorDlq.atualizar();
        assertThat(monitorDlq.mensagensNaFila()).isEqualTo(1);
        assertThat(registry.get("oficina.mensageria.dlq.mensagens").tag("fila", FilasDeTeste.DLQ_OS_EVENTOS)
            .gauge().value()).isEqualTo(1.0);
        FilasDeTeste.esvaziar(FilasDeTeste.DLQ_OS_EVENTOS);
    }

    @Test
    void mensagemForaDoFormatoDoEnvelopeVaiParaADlq() {
        FilasDeTeste.esvaziar(FilasDeTeste.DLQ_OS_EVENTOS);

        FilasDeTeste.publicarCorpo(FilasDeTeste.OS_EVENTOS, "{\"tipo\": \"DiagnosticoIniciado\"}", "DiagnosticoIniciado");

        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> assertThat(FilasDeTeste.mensagensAproximadas(FilasDeTeste.DLQ_OS_EVENTOS))
                .isEqualTo(1));
        FilasDeTeste.esvaziar(FilasDeTeste.DLQ_OS_EVENTOS);
    }

    @Test
    void atributoDeTipoJavaDeOutroServicoNaoInterfereNaLeitura() {
        var cenario = cenario(EstadoSaga.AGUARDANDO_FILA_DIAGNOSTICO, false);
        var mensagemId = UUID.randomUUID();

        FilasDeTeste.publicarCorpo(FilasDeTeste.OS_EVENTOS, FilasDeTeste.envelope(mensagemId, "ExecucaoEnfileirada",
                cenario.sagaId(), cenario.osId(), Map.of("etapa", "DIAGNOSTICO")), "ExecucaoEnfileirada",
            Map.of("JavaType", "com.oficina.execution.infrastructure.messaging.MensagemEnvelope"));

        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> assertThat(estadoSaga(cenario.osId())).isEqualTo("AGUARDANDO_DIAGNOSTICO"));
    }

    @Test
    void eventoDeTipoDesconhecidoEhRejeitadoSemNovaTentativa() {
        var cenario = cenario(EstadoSaga.EM_DIAGNOSTICO, false);

        var mensagemId = FilasDeTeste.publicarEvento("EventoInexistente", cenario.sagaId(), cenario.osId(), null,
            Map.of());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(jdbc.queryForObject(
            "SELECT resultado FROM saga_passos WHERE mensagem_id = ?", String.class, mensagemId))
            .isEqualTo("REJEITADO"));
        assertThat(estadoSaga(cenario.osId())).isEqualTo("EM_DIAGNOSTICO");
    }

    @Test
    void reprovacaoCancelaAExecucaoEDevolveAsPecasAoEstoque() {
        var cenario = cenario(EstadoSaga.AGUARDANDO_APROVACAO, true);

        FilasDeTeste.publicarEvento("OrcamentoReprovado", cenario.sagaId(), cenario.osId(), null,
            Map.of("orcamentoId", UUID.randomUUID().toString(), "reprovadoEm", Instant.now().toString(),
                "canal", "LINK"));
        var cancelamento = FilasDeTeste.aguardarComando(FilasDeTeste.EXECUTION_COMANDOS, cenario.osId(),
            "CancelarExecucao");
        assertThat(cancelamento.envelope().path("dados").path("motivo").asText()).isEqualTo("REPROVACAO");
        assertThat(estadoSaga(cenario.osId())).isEqualTo("COMPENSANDO");
        assertThat(statusOs(cenario.osId())).isEqualTo("AGUARDANDO_APROVACAO");

        FilasDeTeste.publicarEvento("ExecucaoCancelada", cenario.sagaId(), cenario.osId(),
            cancelamento.mensagemId(), Map.of("motivo", "REPROVACAO", "canceladaEm", Instant.now().toString()));

        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> assertThat(estadoSaga(cenario.osId())).isEqualTo("COMPENSADA"));
        assertThat(statusOs(cenario.osId())).isEqualTo("CANCELADA");
        assertThat(jdbc.queryForObject("SELECT quantidade_estoque FROM pecas WHERE id = ?", Integer.class,
            cenario.pecaId())).isEqualTo(10);
        assertThat(jdbc.queryForList("SELECT tipo FROM saga_passos WHERE saga_id = ? ORDER BY sequencia",
            String.class, cenario.sagaId()))
            .containsSubsequence("OrcamentoReprovado", "CancelarExecucao", "ExecucaoCancelada", "ESTORNAR_ESTOQUE");
        assertThat(jdbc.queryForObject("SELECT evento FROM historico_status_os WHERE os_id = ? "
            + "AND status_destino = 'CANCELADA'", String.class, cenario.osId())).isEqualTo("REPROVACAO");
    }

    private record Cenario(UUID osId, UUID sagaId, UUID pecaId) {}

    private Cenario cenario(EstadoSaga estado, boolean comPeca) {
        return unidadeDeTrabalho.executar(() -> {
            var cliente = clienteRepository.salvar(new Cliente(UUID.randomUUID(),
                new CpfCnpj(DocumentosDeTeste.cpfValido()), "Cliente da saga", "cliente@example.com", null));
            var veiculo = veiculoRepository.salvar(new Veiculo(UUID.randomUUID(),
                new Placa(DocumentosDeTeste.placaValida()), "VW", "Gol", 2019, cliente.getId()));
            var peca = new Peca(UUID.randomUUID(), "Pastilha", "", new BigDecimal("80.00"), 10, 1);
            var os = new OrdemServico(UUID.randomUUID(), cliente.getId(), veiculo.getId());
            if (estado != EstadoSaga.AGUARDANDO_FILA_DIAGNOSTICO && estado != EstadoSaga.AGUARDANDO_DIAGNOSTICO) {
                os.registrarInicioDiagnostico();
            }
            if (comPeca) {
                peca.decrementarEstoque(2);
                os.adicionarPeca(new ItemPeca(UUID.randomUUID(), peca.getId(), peca.getNome(),
                    peca.getPrecoUnitario(), 2));
            }
            pecaRepository.salvar(peca);
            if (estado == EstadoSaga.AGUARDANDO_APROVACAO) {
                os.registrarOrcamentoEnviado();
            }
            osRepository.salvar(os);
            var agora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            var saga = SagaOrdemServico.reconstituir(DadosSagaOrdemServico.builder()
                .id(UUID.randomUUID()).osId(os.getId()).estado(estado).criadaEm(agora).atualizadaEm(agora).build());
            sagaRepository.salvar(saga);
            return new Cenario(os.getId(), saga.getId(), peca.getId());
        });
    }

    private String estadoSaga(UUID osId) {
        return jdbc.queryForObject("SELECT estado FROM sagas_ordem_servico WHERE os_id = ?", String.class, osId);
    }

    private String statusOs(UUID osId) {
        return jdbc.queryForObject("SELECT status FROM ordens_servico WHERE id = ?", String.class, osId);
    }
}
