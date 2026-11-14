package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.ordemservico.CompensacaoEstornoEstoque;
import com.oficina.mecanica.domain.entity.DadosOrdemServico;
import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.exception.TransicaoInvalidaException;
import com.oficina.mecanica.domain.repository.PecaRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompensacaoEstornoEstoqueTest {

    @Mock PecaRepository pecaRepository;

    CompensacaoEstornoEstoque compensacao;
    UUID pecaId;

    @BeforeEach
    void setUp() {
        compensacao = new CompensacaoEstornoEstoque(pecaRepository);
        pecaId = UUID.randomUUID();
    }

    @Test
    void devolveAsPecasAoEstoqueECancelaAOrdem() {
        var os = ordem(StatusOS.AGUARDANDO_APROVACAO, 3);
        var peca = new Peca(pecaId, "Filtro", "", new BigDecimal("50.00"), 5, 2);
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));

        var aplicada = compensacao.aplicar(os, "REPROVACAO");

        assertThat(aplicada).isTrue();
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(8);
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(pecaRepository).salvar(peca);
    }

    @Test
    void ordemJaCanceladaNaoEstornaDeNovo() {
        var os = ordem(StatusOS.CANCELADA, 3);

        var aplicada = compensacao.aplicar(os, "REPROVACAO");

        assertThat(aplicada).isFalse();
        verifyNoInteractions(pecaRepository);
    }

    @Test
    void estornoRepetidoDevolveAsPecasUmaUnicaVez() {
        var os = ordem(StatusOS.EM_EXECUCAO, 2);
        var peca = new Peca(pecaId, "Filtro", "", new BigDecimal("50.00"), 5, 2);
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));

        compensacao.aplicar(os, "FALHA_EXECUCAO");
        compensacao.aplicar(os, "FALHA_EXECUCAO");

        assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
        verify(pecaRepository, times(1)).salvar(peca);
    }

    @Test
    void transicaoInvalidaNaoTocaNoEstoque() {
        var os = ordem(StatusOS.FINALIZADA, 2);

        assertThatThrownBy(() -> compensacao.aplicar(os, "FALHA_EXECUCAO"))
            .isInstanceOf(TransicaoInvalidaException.class);
        verifyNoInteractions(pecaRepository);
    }

    @Test
    void pecaRemovidaDoCatalogoEhIgnorada() {
        var os = ordem(StatusOS.AGUARDANDO_PAGAMENTO, 1);
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.empty());

        assertThat(compensacao.aplicar(os, "PAGAMENTO_RECUSADO")).isTrue();
        verify(pecaRepository, never()).salvar(any());
    }

    private OrdemServico ordem(StatusOS status, int quantidade) {
        var item = new ItemPeca(UUID.randomUUID(), pecaId, "Filtro", new BigDecimal("50.00"), quantidade);
        return OrdemServico.reconstituir(DadosOrdemServico.builder()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(status).itensPeca(List.of(item)).dataAbertura(LocalDateTime.now()).build());
    }
}
