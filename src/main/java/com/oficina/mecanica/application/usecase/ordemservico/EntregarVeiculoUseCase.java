package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;

import java.util.UUID;

public class EntregarVeiculoUseCase {

    private final OrdemServicoRepository repository;

    public EntregarVeiculoUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    public OrdemServico executar(UUID osId) {
        var os = repository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
        os.entregar();
        return repository.salvar(os);
    }
}
