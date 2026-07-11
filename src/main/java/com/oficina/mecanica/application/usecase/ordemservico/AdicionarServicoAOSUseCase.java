package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.ItemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.ServicoRepository;

import java.util.UUID;

public class AdicionarServicoAOSUseCase {

    private final OrdemServicoRepository osRepository;
    private final ServicoRepository servicoRepository;

    public AdicionarServicoAOSUseCase(OrdemServicoRepository osRepository,
                                      ServicoRepository servicoRepository) {
        this.osRepository = osRepository;
        this.servicoRepository = servicoRepository;
    }

    public OrdemServico executar(UUID osId, UUID servicoId, int quantidade) {
        var os = osRepository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
        var servico = servicoRepository.buscarPorId(servicoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", servicoId));

        // Snapshot de preço: cópia do valor do catálogo — reajuste futuro não altera esta OS
        var item = new ItemServico(
            UUID.randomUUID(),
            servico.getId(),
            servico.getNome(),
            servico.getPrecoUnitario(),
            quantidade
        );
        os.adicionarServico(item);
        return osRepository.salvar(os);
    }
}
