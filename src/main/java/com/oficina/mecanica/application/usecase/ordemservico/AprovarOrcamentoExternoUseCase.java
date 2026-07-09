package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;

import java.util.UUID;

public class AprovarOrcamentoExternoUseCase {

    private final OrdemServicoRepository repository;

    public AprovarOrcamentoExternoUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    public OrdemServico executar(UUID osId, String token) {
        var os = repository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
        os.aprovarViaTokenExterno(token);
        return repository.salvar(os);
    }
}
