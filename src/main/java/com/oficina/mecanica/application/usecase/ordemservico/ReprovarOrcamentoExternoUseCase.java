package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;

import java.util.UUID;

public class ReprovarOrcamentoExternoUseCase {

    private final OrdemServicoRepository osRepository;
    private final EstornoPecasHelper estornoPecasHelper;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public ReprovarOrcamentoExternoUseCase(OrdemServicoRepository osRepository, PecaRepository pecaRepository,
                                           UnidadeDeTrabalho unidadeDeTrabalho) {
        this.osRepository = osRepository;
        this.estornoPecasHelper = new EstornoPecasHelper(pecaRepository);
        this.unidadeDeTrabalho = unidadeDeTrabalho;
    }

    public OrdemServico executar(UUID osId, String token) {
        return unidadeDeTrabalho.executar(() -> {
            var os = osRepository.buscarPorId(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));

            // Valida o token antes de estornar — evita estorno de estoque em requisições inválidas
            os.reprovarViaTokenExterno(token);
            estornoPecasHelper.estornarPecas(os);

            return osRepository.salvar(os);
        });
    }
}
