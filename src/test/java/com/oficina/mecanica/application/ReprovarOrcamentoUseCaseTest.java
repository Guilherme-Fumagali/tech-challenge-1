package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.ordemservico.ReprovarOrcamentoExternoUseCase;
import com.oficina.mecanica.application.usecase.ordemservico.ReprovarOrcamentoUseCase;
import com.oficina.mecanica.domain.entity.DadosOrdemServico;
import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.exception.TokenAprovacaoInvalidoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReprovarOrcamentoUseCaseTest {

    @Mock OrdemServicoRepository osRepository;
    @Mock PecaRepository pecaRepository;

    ReprovarOrcamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReprovarOrcamentoUseCase(osRepository, pecaRepository);
    }

    @Test
    void deveEstornarPecasAoCancelar() {
        var pecaId = UUID.randomUUID();
        var item = new ItemPeca(UUID.randomUUID(), pecaId, "Filtro", new BigDecimal("50.00"), 3);

        var os = OrdemServico.reconstituir(DadosOrdemServico.builder()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(StatusOS.AGUARDANDO_APROVACAO).itensServico(new ArrayList<>()).itensPeca(List.of(item))
            .build());

        var peca = new Peca(pecaId, "Filtro", "", new BigDecimal("50.00"), 5, 2);

        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));
        when(pecaRepository.salvar(any())).thenReturn(peca);
        when(osRepository.salvar(any())).thenReturn(os);

        useCase.executar(os.getId());

        // Estoque deve ter sido devolvido: 5 + 3 = 8
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(8);
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(pecaRepository).salvar(peca);
    }

    @Test
    void reprovarExterno_deveEstornarPecasEInvalidarToken() {
        var pecaId = UUID.randomUUID();
        var item = new ItemPeca(UUID.randomUUID(), pecaId, "Filtro", new BigDecimal("50.00"), 3);

        var os = OrdemServico.reconstituir(DadosOrdemServico.builder()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(StatusOS.AGUARDANDO_APROVACAO).itensServico(new ArrayList<>()).itensPeca(List.of(item))
            .tokenAprovacaoExterna("token-valido").tokenExpiracao(LocalDateTime.now(ZoneOffset.UTC).plusHours(1))
            .build());

        var peca = new Peca(pecaId, "Filtro", "", new BigDecimal("50.00"), 5, 2);

        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));
        when(pecaRepository.salvar(any())).thenReturn(peca);
        when(osRepository.salvar(any())).thenReturn(os);

        new ReprovarOrcamentoExternoUseCase(osRepository, pecaRepository).executar(os.getId(), "token-valido");

        assertThat(peca.getQuantidadeEstoque()).isEqualTo(8);
        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        assertThat(os.getTokenAprovacaoExterna()).isNull();
    }

    @Test
    void reprovarExterno_deveLancarExcecaoENaoEstornarSeTokenInvalido() {
        var pecaId = UUID.randomUUID();
        var item = new ItemPeca(UUID.randomUUID(), pecaId, "Filtro", new BigDecimal("50.00"), 3);

        var os = OrdemServico.reconstituir(DadosOrdemServico.builder()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(StatusOS.AGUARDANDO_APROVACAO).itensServico(new ArrayList<>()).itensPeca(List.of(item))
            .tokenAprovacaoExterna("token-valido").tokenExpiracao(LocalDateTime.now(ZoneOffset.UTC).plusHours(1))
            .build());

        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));

        var uc = new ReprovarOrcamentoExternoUseCase(osRepository, pecaRepository);
        var osId = os.getId();
        // A mensagem faz parte da asserção de propósito: "expirado" lança esta mesma exceção.
        // Checar só o tipo deixaria o teste passar por token vencido, sem provar nada sobre a
        // verificação do token — que é o que ele diz estar testando.
        assertThatThrownBy(() -> uc.executar(osId, "token-errado"))
            .isInstanceOf(TokenAprovacaoInvalidoException.class)
            .hasMessageContaining("inválido");

        verifyNoInteractions(pecaRepository);
    }
}
