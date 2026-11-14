package com.oficina.mecanica.domain;

import com.oficina.mecanica.domain.entity.DadosOrdemServico;
import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.ItemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.TransicaoInvalidaException;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrdemServicoTest {

    private static final Instant APROVADA_EM = Instant.parse("2026-10-30T14:05:11Z");
    private static final Instant PAGA_EM = Instant.parse("2026-10-30T15:00:00Z");
    private static final Instant CONCLUIDA_EM = Instant.parse("2026-10-31T10:30:00Z");

    private UUID clienteId;
    private UUID veiculoId;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {
        @Test
        void deveCriarOsComStatusRecebida() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            assertThat(os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(os.getItensServico()).isEmpty();
            assertThat(os.getItensPeca()).isEmpty();
            assertThat(os.getVersao()).isNull();
            assertThat(os.consumirMarcaDeNovaOrdem()).isTrue();
            assertThat(os.consumirMarcaDeNovaOrdem()).isFalse();
        }

        @Test
        void reconstituirPreservaVersaoEDatas() {
            var abertura = LocalDateTime.of(2026, 10, 1, 8, 0);
            var os = OrdemServico.reconstituir(DadosOrdemServico.builder()
                .id(UUID.randomUUID()).clienteId(clienteId).veiculoId(veiculoId)
                .status(StatusOS.EM_DIAGNOSTICO).dataAbertura(abertura).versao(3L).build());

            assertThat(os.getVersao()).isEqualTo(3L);
            assertThat(os.getDataUltimaTransicao()).isEqualTo(abertura);
            assertThat(os.consumirMarcaDeNovaOrdem()).isFalse();
        }
    }

    @Nested
    @DisplayName("Fluxo de status")
    class FluxoStatus {

        @Test
        void deveAvancarStatusNaOrdemCorreta() {
            var os = criarOsComItem();

            os.registrarOrcamentoEnviado();
            assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);

            os.registrarAprovacao(APROVADA_EM);
            assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_PAGAMENTO);
            assertThat(os.getDataAprovacao()).isEqualTo(LocalDateTime.ofInstant(APROVADA_EM, ZoneOffset.UTC));

            os.registrarPagamento(PAGA_EM);
            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
            assertThat(os.getDataInicio()).isEqualTo(LocalDateTime.ofInstant(PAGA_EM, ZoneOffset.UTC));

            os.registrarConclusao(CONCLUIDA_EM);
            assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
            assertThat(os.getDataConclusao()).isEqualTo(LocalDateTime.ofInstant(CONCLUIDA_EM, ZoneOffset.UTC));

            os.entregar();
            assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
            assertThat(os.isExcluidaLogicamente()).isFalse();
        }

        @Test
        void transicoesPendentesRegistramOrigemDestinoEEvento() {
            var os = criarOsComItem();
            os.registrarOrcamentoEnviado();

            var transicoes = os.drenarTransicoes();

            assertThat(transicoes).extracting("origem", "destino", "evento").containsExactly(
                tuple(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO, "DiagnosticoIniciado"),
                tuple(StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO, "OrcamentoGerado"));
            assertThat(transicoes).allMatch(t -> t.alterouStatus() && t.ocorridaEm() != null);
            assertThat(os.drenarTransicoes()).isEmpty();
        }

        @Test
        void dataDoFatoAusenteUsaOInstanteAtual() {
            var os = criarOsComItem();
            os.registrarOrcamentoEnviado();

            os.registrarAprovacao(null);

            assertThat(os.getDataAprovacao()).isNotNull();
        }

        @Test
        void cancelamentoRegistraOMotivoComoEvento() {
            var os = criarOsComItem();
            os.registrarOrcamentoEnviado();
            os.drenarTransicoes();

            os.cancelar("REPROVACAO");

            assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
            assertThat(os.drenarTransicoes()).singleElement()
                .satisfies(t -> assertThat(t.evento()).isEqualTo("REPROVACAO"));
        }

        @Test
        void cancelamentoExigeMotivo() {
            var os = criarOsComItem();

            assertThatThrownBy(() -> os.cancelar(" "))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("motivo");
            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        }

        @Test
        void deveRejeitarTransicaoInvalida() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            assertThatThrownBy(() -> os.registrarPagamento(PAGA_EM))
                .isInstanceOf(TransicaoInvalidaException.class);
        }

        @Test
        void naoDevePermitirRetroagirStatus() {
            var os = criarOsComItem();
            os.registrarOrcamentoEnviado();
            os.registrarAprovacao(APROVADA_EM);
            assertThatThrownBy(os::registrarInicioDiagnostico)
                .isInstanceOf(TransicaoInvalidaException.class);
        }

        @Test
        void estadoFinalNaoPermiteNenhumaTransicao() {
            var os = criarOsComItem();
            os.cancelar("REJEICAO");
            assertThatThrownBy(os::entregar)
                .isInstanceOf(TransicaoInvalidaException.class);
        }

        @Test
        void ordemFinalizadaNaoPodeSerCancelada() {
            var os = criarOsComItem();
            os.registrarOrcamentoEnviado();
            os.registrarAprovacao(APROVADA_EM);
            os.registrarPagamento(PAGA_EM);
            os.registrarConclusao(CONCLUIDA_EM);

            assertThatThrownBy(() -> os.cancelar("FALHA_EXECUCAO"))
                .isInstanceOf(TransicaoInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("Orçamento")
    class Orcamento {

        @Test
        void deveCalcularOrcamentoCorretamente() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            os.registrarInicioDiagnostico();

            os.adicionarServico(new ItemServico(UUID.randomUUID(), UUID.randomUUID(),
                "Alinhamento", new BigDecimal("150.00"), 2));
            os.adicionarPeca(new ItemPeca(UUID.randomUUID(), UUID.randomUUID(),
                "Filtro de óleo", new BigDecimal("45.50"), 3));

            assertThat(os.calcularOrcamento()).isEqualByComparingTo("436.50");
            assertThat(os.possuiItens()).isTrue();
        }

        @Test
        void deveRejeitarOrcamentoEnviadoSemItens() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            os.registrarInicioDiagnostico();

            assertThat(os.possuiItens()).isFalse();
            assertThatThrownBy(os::registrarOrcamentoEnviado)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ao menos um item");
        }

        @Test
        void deveRejeitarOrcamentoEnviadoForaDoDiagnostico() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);

            assertThatThrownBy(os::registrarOrcamentoEnviado)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RECEBIDA");
        }

        @Test
        void deveRejeitarAdicionarItemForaDoDiagnostico() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);

            var item = new ItemServico(UUID.randomUUID(), UUID.randomUUID(),
                "Troca de óleo", new BigDecimal("80.00"), 1);
            assertThatThrownBy(() -> os.adicionarServico(item))
                .isInstanceOf(DomainException.class);
            var peca = new ItemPeca(UUID.randomUUID(), UUID.randomUUID(), "Vela", new BigDecimal("20.00"), 4);
            assertThatThrownBy(() -> os.adicionarPeca(peca))
                .isInstanceOf(DomainException.class);
        }

        @Test
        void listasDeItensSaoSomenteLeitura() {
            var os = criarOsComItem();

            var itens = os.getItensServico();
            var pecas = os.getItensPeca();
            assertThatThrownBy(() -> itens.add(null)).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> pecas.add(null)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(List.copyOf(itens)).hasSize(1);
        }
    }

    private OrdemServico criarOsComItem() {
        var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
        os.registrarInicioDiagnostico();
        os.adicionarServico(new ItemServico(UUID.randomUUID(), UUID.randomUUID(),
            "Revisão geral", new BigDecimal("200.00"), 1));
        return os;
    }
}
