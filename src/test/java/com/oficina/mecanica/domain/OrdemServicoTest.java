package com.oficina.mecanica.domain;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrdemServicoTest {

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
        }
    }

    @Nested
    @DisplayName("Fluxo de status")
    class FluxoStatus {

        @Test
        void deveAvancarStatusNaOrdemCorreta() {
            var os = criarOsComItem(); // já em EM_DIAGNOSTICO com um item

            os.gerarOrcamento();
            assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);

            os.aprovar();
            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
            assertThat(os.getDataInicio()).isNotNull();

            os.concluir();
            assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
            assertThat(os.getDataConclusao()).isNotNull();

            os.entregar();
            assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
        }

        @Test
        void deveCancelarAoReprovarOrcamento() {
            var os = criarOsComItem(); // já em EM_DIAGNOSTICO
            os.gerarOrcamento();
            os.reprovar();
            assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        }

        @Test
        void deveRejeitarTransicaoInvalida() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            // Recebida não pode ir direto para EM_EXECUCAO
            assertThatThrownBy(os::aprovar)
                .isInstanceOf(TransicaoInvalidaException.class);
        }

        @Test
        void naoDevePermitirRetroagirStatus() {
            var os = criarOsComItem(); // já em EM_DIAGNOSTICO
            os.gerarOrcamento();
            os.aprovar();
            // EM_EXECUCAO não pode voltar para EM_DIAGNOSTICO
            assertThatThrownBy(os::iniciarDiagnostico)
                .isInstanceOf(TransicaoInvalidaException.class);
        }

        @Test
        void estadoFinalNaoPermiteNenhumaTransicao() {
            var os = criarOsComItem(); // já em EM_DIAGNOSTICO
            os.gerarOrcamento();
            os.reprovar();
            // CANCELADA é estado final
            assertThatThrownBy(os::entregar)
                .isInstanceOf(TransicaoInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("Orçamento")
    class Orcamento {

        @Test
        void deveCalcularOrcamentoCorretamente() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            os.iniciarDiagnostico();

            os.adicionarServico(new ItemServico(UUID.randomUUID(), UUID.randomUUID(),
                "Alinhamento", new BigDecimal("150.00"), 2));
            os.adicionarPeca(new ItemPeca(UUID.randomUUID(), UUID.randomUUID(),
                "Filtro de óleo", new BigDecimal("45.50"), 3));

            // 2 * 150 + 3 * 45.50 = 300 + 136.50 = 436.50
            assertThat(os.calcularOrcamento()).isEqualByComparingTo("436.50");
        }

        @Test
        void deveRejeitarGerarOrcamentoSemItens() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            os.iniciarDiagnostico();

            assertThatThrownBy(os::gerarOrcamento)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ao menos um item");
        }

        @Test
        void deveRejeitarAdicionarItemForaDosDiagnostico() {
            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            // Status é RECEBIDA — não permitido adicionar serviço

            var item = new ItemServico(UUID.randomUUID(), UUID.randomUUID(),
                "Troca de óleo", new BigDecimal("80.00"), 1);
            assertThatThrownBy(() -> os.adicionarServico(item))
                .isInstanceOf(DomainException.class);
        }
    }

    // Helper: OS já em diagnóstico com um item de serviço
    private OrdemServico criarOsComItem() {
        var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
        os.iniciarDiagnostico();
        os.adicionarServico(new ItemServico(UUID.randomUUID(), UUID.randomUUID(),
            "Revisão geral", new BigDecimal("200.00"), 1));
        return os;
    }
}
