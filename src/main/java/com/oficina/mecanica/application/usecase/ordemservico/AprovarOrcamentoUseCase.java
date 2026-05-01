package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;

import java.util.UUID;

public class AprovarOrcamentoUseCase {

    private final OrdemServicoRepository repository;

    public AprovarOrcamentoUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    public OrdemServico executar(UUID osId) {
        var os = repository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
        os.aprovar();
        return repository.salvar(os);
    }
}
