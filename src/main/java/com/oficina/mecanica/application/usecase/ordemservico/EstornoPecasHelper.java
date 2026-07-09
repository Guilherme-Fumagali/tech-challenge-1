package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.repository.PecaRepository;

class EstornoPecasHelper {

    private final PecaRepository pecaRepository;

    EstornoPecasHelper(PecaRepository pecaRepository) {
        this.pecaRepository = pecaRepository;
    }

    void estornarPecas(OrdemServico os) {
        os.getItensPeca().forEach(item -> {
            var peca = pecaRepository.buscarPorId(item.getPecaId());
            peca.ifPresent(p -> {
                p.incrementarEstoque(item.getQuantidade());
                pecaRepository.salvar(p);
            });
        });
    }
}
