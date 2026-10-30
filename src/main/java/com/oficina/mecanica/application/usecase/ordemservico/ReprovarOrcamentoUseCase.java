package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;

import java.util.UUID;

public class ReprovarOrcamentoUseCase {

    private final OrdemServicoRepository osRepository;
    private final EstornoPecasHelper estornoPecasHelper;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public ReprovarOrcamentoUseCase(OrdemServicoRepository osRepository, PecaRepository pecaRepository,
                                    UnidadeDeTrabalho unidadeDeTrabalho) {
        this.osRepository = osRepository;
        this.estornoPecasHelper = new EstornoPecasHelper(pecaRepository);
        this.unidadeDeTrabalho = unidadeDeTrabalho;
    }

    public OrdemServico executar(UUID osId) {
        return unidadeDeTrabalho.executar(() -> {
            var os = osRepository.buscarPorId(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));

            estornoPecasHelper.estornarPecas(os);

            os.reprovar();
            return osRepository.salvar(os);
        });
    }
}
