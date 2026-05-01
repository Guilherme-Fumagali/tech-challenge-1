package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;

import java.util.UUID;

public class ConcluirServicosUseCase {

    private final OrdemServicoRepository repository;

    public ConcluirServicosUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    public OrdemServico executar(UUID osId) {
        var os = repository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
        os.concluir();
        return repository.salvar(os);
    }
}
