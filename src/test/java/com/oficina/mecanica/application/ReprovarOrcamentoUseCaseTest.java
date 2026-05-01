package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.ordemservico.ReprovarOrcamentoUseCase;
import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

        var os = new OrdemServico(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            StatusOS.AGUARDANDO_APROVACAO, new ArrayList<>(), List.of(item), null, null, null);

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
}
