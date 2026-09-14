package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;

import java.util.List;
import java.util.UUID;

public class ConsultarStatusOSUseCase {

    private final OrdemServicoRepository repository;

    public ConsultarStatusOSUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    public OrdemServico executar(UUID osId) {
        return repository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
    }

    public List<OrdemServico> listarTodas() {
        return repository.listarAtivasOrdenadas();
    }

    public OrdemServico executarDoCliente(UUID osId, UUID clienteId) {
        var os = executar(osId);
        if (!os.getClienteId().equals(clienteId)) {
            throw new RecursoNaoEncontradoException("Ordem de Serviço", osId);
        }
        return os;
    }

    public List<OrdemServico> listarDoCliente(UUID clienteId) {
        return repository.listarPorCliente(clienteId);
    }
}
