package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;

import java.util.UUID;

public class IniciarDiagnosticoUseCase {

    private final OrdemServicoRepository repository;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public IniciarDiagnosticoUseCase(OrdemServicoRepository repository, UnidadeDeTrabalho unidadeDeTrabalho) {
        this.repository = repository;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
    }

    public OrdemServico executar(UUID osId) {
        return unidadeDeTrabalho.executar(() -> {
            var os = repository.buscarPorId(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
            os.iniciarDiagnostico();
            return repository.salvar(os);
        });
    }
}
