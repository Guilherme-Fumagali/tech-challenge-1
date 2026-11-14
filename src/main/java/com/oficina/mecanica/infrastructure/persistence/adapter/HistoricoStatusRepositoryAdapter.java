package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.HistoricoStatus;
import com.oficina.mecanica.domain.repository.HistoricoStatusRepository;
import com.oficina.mecanica.infrastructure.persistence.repository.HistoricoStatusOSJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class HistoricoStatusRepositoryAdapter implements HistoricoStatusRepository {

    private final HistoricoStatusOSJpaRepository jpa;

    public HistoricoStatusRepositoryAdapter(HistoricoStatusOSJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<HistoricoStatus> listarPorOrdemServico(UUID ordemServicoId) {
        return jpa.findByOsIdOrderByOcorridoEmAsc(ordemServicoId).stream()
            .map(h -> new HistoricoStatus(h.getOsId(), h.getStatusOrigem(), h.getStatusDestino(), h.getEvento(),
                h.getOcorridoEm()))
            .toList();
    }
}
