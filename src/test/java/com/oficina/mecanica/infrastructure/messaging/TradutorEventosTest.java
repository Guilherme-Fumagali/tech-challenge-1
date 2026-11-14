package com.oficina.mecanica.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.domain.saga.EtapaExecucao;
import com.oficina.mecanica.domain.saga.TipoEvento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TradutorEventosTest {

    private static final Instant OCORRIDO = Instant.parse("2026-10-30T14:00:00Z");

    private final ObjectMapper json = new ObjectMapper();
    private final TradutorEventos tradutor = new TradutorEventos();

    @ParameterizedTest
    @CsvSource({
        "EXECUCAO_ENFILEIRADA, enfileiradaEm",
        "DIAGNOSTICO_INICIADO, iniciadoEm",
        "DIAGNOSTICO_CONCLUIDO, concluidoEm",
        "REPARO_INICIADO, iniciadoEm",
        "EXECUCAO_FINALIZADA, finalizadaEm",
        "EXECUCAO_FALHOU, ocorridaEm",
        "EXECUCAO_CANCELADA, canceladaEm",
        "ORCAMENTO_APROVADO, aprovadoEm",
        "ORCAMENTO_REPROVADO, reprovadoEm",
        "ORCAMENTO_EXPIRADO, expiradoEm",
        "PAGAMENTO_APROVADO, aprovadoEm",
        "PAGAMENTO_ESTORNADO, estornadoEm"
    })
    void instanteDoFatoVemDoCampoDoContrato(TipoEvento tipo, String campo) {
        var evento = tradutor.traduzir(envelope(tipo, Map.of(campo, "2026-10-30T13:59:00Z")), tipo);

        assertThat(evento.instanteDoFato()).isEqualTo(Instant.parse("2026-10-30T13:59:00Z"));
        assertThat(evento.ocorridoEm()).isEqualTo(OCORRIDO);
        assertThat(evento.tipo()).isEqualTo(tipo);
    }

    @Test
    void eventosSemInstanteProprioUsamAOcorrencia() {
        var evento = tradutor.traduzir(envelope(TipoEvento.ORCAMENTO_GERADO,
            Map.of("orcamentoId", UUID.randomUUID().toString(), "total", 210.0)), TipoEvento.ORCAMENTO_GERADO);

        assertThat(evento.instanteDoFato()).isNull();
        assertThat(evento.instanteDoFatoOuOcorrencia()).isEqualTo(OCORRIDO);
    }

    @Test
    void enfileiramentoInformaAEtapa() {
        var evento = tradutor.traduzir(envelope(TipoEvento.EXECUCAO_ENFILEIRADA, Map.of("etapa", "REPARO")),
            TipoEvento.EXECUCAO_ENFILEIRADA);

        assertThat(evento.etapa()).isEqualTo(EtapaExecucao.REPARO);
        assertThat(evento.detalhe()).isEqualTo("REPARO");
    }

    @Test
    void etapaDesconhecidaEInstanteInvalidoViramAusentes() {
        var evento = tradutor.traduzir(envelope(TipoEvento.EXECUCAO_ENFILEIRADA,
            Map.of("etapa", "PINTURA", "enfileiradaEm", "ontem")), TipoEvento.EXECUCAO_ENFILEIRADA);

        assertThat(evento.etapa()).isNull();
        assertThat(evento.instanteDoFato()).isNull();
    }

    @Test
    void rejeicaoInformaComandoMotivoECodigo() {
        var evento = tradutor.traduzir(envelope(TipoEvento.COMANDO_REJEITADO,
            Map.of("comando", "GerarOrcamento", "motivo", "cliente sem e-mail", "codigo", "DADOS_INVALIDOS")),
            TipoEvento.COMANDO_REJEITADO);

        assertThat(evento.comandoRejeitado()).isEqualTo("GerarOrcamento");
        assertThat(evento.detalhe()).isEqualTo("cliente sem e-mail (DADOS_INVALIDOS)");
    }

    @Test
    void rejeicaoSemMotivoOuSemCodigo() {
        var soCodigo = tradutor.traduzir(envelope(TipoEvento.COMANDO_REJEITADO, Map.of("codigo", "X")),
            TipoEvento.COMANDO_REJEITADO);
        var soMotivo = tradutor.traduzir(envelope(TipoEvento.COMANDO_REJEITADO, Map.of("motivo", "m")),
            TipoEvento.COMANDO_REJEITADO);

        assertThat(soCodigo.detalhe()).isEqualTo("X");
        assertThat(soMotivo.detalhe()).isEqualTo("m");
    }

    @Test
    void detalheLongoEhTruncadoEObservacoesDoDiagnosticoSaoMantidas() {
        var longo = "a".repeat(600);
        var falha = tradutor.traduzir(envelope(TipoEvento.EXECUCAO_FALHOU, Map.of("motivo", longo)),
            TipoEvento.EXECUCAO_FALHOU);
        var diagnostico = tradutor.traduzir(envelope(TipoEvento.DIAGNOSTICO_CONCLUIDO,
            Map.of("observacoes", "pastilhas gastas")), TipoEvento.DIAGNOSTICO_CONCLUIDO);

        assertThat(falha.detalhe()).hasSize(500);
        assertThat(diagnostico.detalhe()).isEqualTo("pastilhas gastas");
    }

    @Test
    void envelopeSemDadosNaoQuebraATraducao() {
        var envelope = new MensagemEnvelope(UUID.randomUUID(), "PagamentoRecusado", 1, UUID.randomUUID(),
            UUID.randomUUID(), null, "billing-service", OCORRIDO, null);

        var evento = tradutor.traduzir(envelope, TipoEvento.PAGAMENTO_RECUSADO);

        assertThat(evento.detalhe()).isNull();
        assertThat(evento.etapa()).isNull();
        assertThat(evento.comandoRejeitado()).isNull();
    }

    private MensagemEnvelope envelope(TipoEvento tipo, Map<String, Object> dados) {
        return new MensagemEnvelope(UUID.randomUUID(), tipo.nome(), 1, UUID.randomUUID(), UUID.randomUUID(), null,
            "execution-service", OCORRIDO, json.valueToTree(dados));
    }
}
