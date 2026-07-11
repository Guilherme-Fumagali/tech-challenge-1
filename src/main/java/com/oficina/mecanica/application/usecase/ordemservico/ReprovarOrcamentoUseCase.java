package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;

import java.util.UUID;

public class ReprovarOrcamentoUseCase {

    private final OrdemServicoRepository osRepository;
    private final EstornoPecasHelper estornoPecasHelper;

    public ReprovarOrcamentoUseCase(OrdemServicoRepository osRepository, PecaRepository pecaRepository) {
        this.osRepository = osRepository;
        this.estornoPecasHelper = new EstornoPecasHelper(pecaRepository);
    }

    public OrdemServico executar(UUID osId) {
        var os = osRepository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));

        estornoPecasHelper.estornarPecas(os);

        os.reprovar();
        return osRepository.salvar(os);
    }
}
