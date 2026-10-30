package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.HistoricoStatus;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.HistoricoStatusRepository;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;

import java.util.List;
import java.util.UUID;

public class ConsultarHistoricoOSUseCase {

    private final OrdemServicoRepository osRepository;
    private final HistoricoStatusRepository historicoRepository;

    public ConsultarHistoricoOSUseCase(OrdemServicoRepository osRepository,
                                       HistoricoStatusRepository historicoRepository) {
        this.osRepository = osRepository;
        this.historicoRepository = historicoRepository;
    }

    public List<HistoricoStatus> executar(UUID osId) {
        if (!osRepository.existePorId(osId)) {
            throw new RecursoNaoEncontradoException("Ordem de Serviço", osId);
        }
        return historicoRepository.listarPorOrdemServico(osId);
    }

    public List<HistoricoStatus> executarDoCliente(UUID osId, UUID clienteId) {
        var os = osRepository.buscarPorId(osId)
            .filter(ordem -> ordem.getClienteId().equals(clienteId))
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
        return historicoRepository.listarPorOrdemServico(os.getId());
    }
}
