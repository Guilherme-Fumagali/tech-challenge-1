package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.saga.MontadorDadosComando;
import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.entity.DadosOrdemServico;
import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.ItemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.VeiculoRepository;
import com.oficina.mecanica.domain.saga.ComandoSaga;
import com.oficina.mecanica.domain.saga.TipoComando;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.domain.valueobject.Placa;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MontadorDadosComandoTest {

    @Mock ClienteRepository clienteRepository;
    @Mock VeiculoRepository veiculoRepository;

    MontadorDadosComando montador;
    OrdemServico os;
    UUID servicoId;
    UUID pecaId;

    @BeforeEach
    void setUp() {
        montador = new MontadorDadosComando(clienteRepository, veiculoRepository);
        servicoId = UUID.randomUUID();
        pecaId = UUID.randomUUID();
        os = OrdemServico.reconstituir(DadosOrdemServico.builder()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(StatusOS.EM_DIAGNOSTICO).dataAbertura(LocalDateTime.of(2026, 10, 30, 14, 5, 11, 482_000_000))
            .itensServico(List.of(new ItemServico(UUID.randomUUID(), servicoId, "Troca de óleo", new BigDecimal("120"), 1)))
            .itensPeca(List.of(new ItemPeca(UUID.randomUUID(), pecaId, "Filtro", new BigDecimal("45.5"), 2)))
            .build());
    }

    @Test
    void iniciarDiagnosticoLevaClienteVeiculoEAbertura() {
        quandoVeiculoExiste();

        var dados = montador.montar(new ComandoSaga(TipoComando.INICIAR_DIAGNOSTICO, null), os);

        assertThat(dados).containsEntry("clienteId", os.getClienteId().toString())
            .containsEntry("abertaEm", "2026-10-30T14:05:11.482Z");
        assertThat(dados.get("veiculo")).isEqualTo(Map.of("placa", "ABC1D23", "marca", "Fiat", "modelo", "Argo",
            "anoFabricacao", 2023));
    }

    @Test
    void gerarOrcamentoLevaOSnapshotDosItensComDuasCasas() {
        quandoVeiculoExiste();
        when(clienteRepository.buscarPorId(os.getClienteId())).thenReturn(Optional.of(new Cliente(os.getClienteId(),
            new CpfCnpj("529.982.247-25"), "Maria", "maria@example.com", null)));

        var dados = montador.montar(new ComandoSaga(TipoComando.GERAR_ORCAMENTO, null), os);

        assertThat(dados.get("cliente")).isEqualTo(Map.of("nome", "Maria", "email", "maria@example.com"));
        assertThat(dados.get("veiculo")).isEqualTo(Map.of("placa", "ABC1D23", "modelo", "Argo"));
        @SuppressWarnings("unchecked")
        var itens = (List<Map<String, Object>>) dados.get("itens");
        assertThat(itens).hasSize(2);
        assertThat(itens.get(0)).containsEntry("tipo", "SERVICO").containsEntry("referenciaId", servicoId.toString())
            .containsEntry("quantidade", 1).containsEntry("precoUnitario", new BigDecimal("120.00"));
        assertThat(itens.get(1)).containsEntry("tipo", "PECA").containsEntry("descricao", "Filtro")
            .containsEntry("precoUnitario", new BigDecimal("45.50"));
    }

    @Test
    void iniciarReparoListaOsItensSemPreco() {
        var dados = montador.montar(new ComandoSaga(TipoComando.INICIAR_REPARO, null), os);

        assertThat(dados.get("itens")).isEqualTo(List.of(
            Map.of("tipo", "SERVICO", "descricao", "Troca de óleo", "quantidade", 1),
            Map.of("tipo", "PECA", "descricao", "Filtro", "quantidade", 2)));
    }

    @Test
    void comandosDeCompensacaoLevamOMotivo() {
        assertThat(montador.montar(new ComandoSaga(TipoComando.CANCELAR_EXECUCAO, "REPROVACAO"), os))
            .isEqualTo(Map.of("motivo", "REPROVACAO"));
        assertThat(montador.montar(new ComandoSaga(TipoComando.ESTORNAR_PAGAMENTO, null), os))
            .isEqualTo(Map.of("motivo", ""));
    }

    @Test
    void veiculoInexistenteImpedeOComando() {
        when(veiculoRepository.buscarPorId(os.getVeiculoId())).thenReturn(Optional.empty());
        var comando = new ComandoSaga(TipoComando.INICIAR_DIAGNOSTICO, null);

        assertThatThrownBy(() -> montador.montar(comando, os)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void clienteInexistenteImpedeOPedidoDeOrcamento() {
        when(clienteRepository.buscarPorId(os.getClienteId())).thenReturn(Optional.empty());
        var comando = new ComandoSaga(TipoComando.GERAR_ORCAMENTO, null);

        assertThatThrownBy(() -> montador.montar(comando, os)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private void quandoVeiculoExiste() {
        when(veiculoRepository.buscarPorId(os.getVeiculoId())).thenReturn(Optional.of(new Veiculo(os.getVeiculoId(),
            new Placa("ABC1D23"), "Fiat", "Argo", 2023, os.getClienteId())));
    }
}
