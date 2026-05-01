package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;

import java.util.UUID;

public class ReprovarOrcamentoUseCase {

    private final OrdemServicoRepository osRepository;
    private final PecaRepository pecaRepository;

    public ReprovarOrcamentoUseCase(OrdemServicoRepository osRepository, PecaRepository pecaRepository) {
        this.osRepository = osRepository;
        this.pecaRepository = pecaRepository;
    }

    public OrdemServico executar(UUID osId) {
        var os = osRepository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));

        // Estorno de Estoque: devolve as peças reservadas antes de cancelar
        estornarPecas(os);

        os.reprovar();
        return osRepository.salvar(os);
    }

    private void estornarPecas(OrdemServico os) {
        os.getItensPeca().forEach(item -> {
            var peca = pecaRepository.buscarPorId(item.getPecaId());
            peca.ifPresent(p -> {
                p.incrementarEstoque(item.getQuantidade());
                pecaRepository.salvar(p);
            });
        });
    }
}
