package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.ordemservico.AdicionarPecaAOSUseCase;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.exception.EstoqueInsuficienteException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdicionarPecaAOSUseCaseTest {

    @Mock OrdemServicoRepository osRepository;
    @Mock PecaRepository pecaRepository;

    AdicionarPecaAOSUseCase useCase;

    UUID osId;
    UUID pecaId;

    @BeforeEach
    void setUp() {
        useCase = new AdicionarPecaAOSUseCase(osRepository, pecaRepository);
        osId = UUID.randomUUID();
        pecaId = UUID.randomUUID();
    }

    @Test
    void deveAdicionarPecaEDecrementarEstoque() {
        var os = osEmDiagnostico();
        var peca = new Peca(pecaId, "Filtro", "", new BigDecimal("50.00"), 10, 2);

        when(osRepository.buscarPorId(osId)).thenReturn(Optional.of(os));
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));
        when(pecaRepository.salvar(any())).thenReturn(peca);
        when(osRepository.salvar(any())).thenReturn(os);

        var resultado = useCase.executar(osId, pecaId, 3);

        assertThat(resultado.getItensPeca()).hasSize(1);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
        verify(pecaRepository).salvar(peca);
    }

    @Test
    void deveLancarExcecaoSeEstoqueInsuficiente() {
        var os = osEmDiagnostico();
        var peca = new Peca(pecaId, "Filtro", "", new BigDecimal("50.00"), 1, 0);

        when(osRepository.buscarPorId(osId)).thenReturn(Optional.of(os));
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));

        assertThatThrownBy(() -> useCase.executar(osId, pecaId, 5))
            .isInstanceOf(EstoqueInsuficienteException.class);

        verify(osRepository, never()).salvar(any());
    }

    @Test
    void deveLancarExcecaoSeOsNaoEncontrada() {
        when(osRepository.buscarPorId(osId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(osId, pecaId, 1))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private OrdemServico osEmDiagnostico() {
        var os = new OrdemServico(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            StatusOS.EM_DIAGNOSTICO, new ArrayList<>(), new ArrayList<>(), null, null, null);
        return os;
    }
}
