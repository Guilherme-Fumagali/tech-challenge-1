package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;

import java.util.UUID;

public class AdicionarPecaAOSUseCase {

    private final OrdemServicoRepository osRepository;
    private final PecaRepository pecaRepository;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public AdicionarPecaAOSUseCase(OrdemServicoRepository osRepository, PecaRepository pecaRepository,
                                   UnidadeDeTrabalho unidadeDeTrabalho) {
        this.osRepository = osRepository;
        this.pecaRepository = pecaRepository;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
    }

    public OrdemServico executar(UUID osId, UUID pecaId, int quantidade) {
        return unidadeDeTrabalho.executar(() -> {
            var os = osRepository.buscarPorId(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
            var peca = pecaRepository.buscarPorId(pecaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", pecaId));

            peca.decrementarEstoque(quantidade);

            var item = new ItemPeca(
                UUID.randomUUID(),
                peca.getId(),
                peca.getNome(),
                peca.getPrecoUnitario(),
                quantidade
            );
            os.adicionarPeca(item);

            pecaRepository.salvar(peca);
            return osRepository.salvar(os);
        });
    }
}
