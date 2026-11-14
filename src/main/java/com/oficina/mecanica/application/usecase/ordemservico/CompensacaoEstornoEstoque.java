package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.repository.PecaRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;

public class CompensacaoEstornoEstoque {

    private final PecaRepository pecaRepository;

    public CompensacaoEstornoEstoque(PecaRepository pecaRepository) {
        this.pecaRepository = pecaRepository;
    }

    public boolean aplicar(OrdemServico os, String motivo) {
        if (os.getStatus() == StatusOS.CANCELADA) {
            return false;
        }
        os.cancelar(motivo);
        os.getItensPeca().forEach(item -> pecaRepository.buscarPorId(item.getPecaId()).ifPresent(peca -> {
            peca.incrementarEstoque(item.getQuantidade());
            pecaRepository.salvar(peca);
        }));
        return true;
    }
}
