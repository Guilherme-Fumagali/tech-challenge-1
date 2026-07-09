package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;

import java.util.UUID;

public class ReprovarOrcamentoExternoUseCase {

    private final OrdemServicoRepository osRepository;
    private final EstornoPecasHelper estornoPecasHelper;

    public ReprovarOrcamentoExternoUseCase(OrdemServicoRepository osRepository, PecaRepository pecaRepository) {
        this.osRepository = osRepository;
        this.estornoPecasHelper = new EstornoPecasHelper(pecaRepository);
    }

    public OrdemServico executar(UUID osId, String token) {
        var os = osRepository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));

        // Valida o token antes de estornar — evita estorno de estoque em requisições inválidas
        os.reprovarViaTokenExterno(token);
        estornoPecasHelper.estornarPecas(os);

        return osRepository.salvar(os);
    }
}
