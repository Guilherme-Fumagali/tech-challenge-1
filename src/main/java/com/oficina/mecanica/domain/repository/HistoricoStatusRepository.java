package com.oficina.mecanica.domain.repository;

import com.oficina.mecanica.domain.entity.HistoricoStatus;

import java.util.List;
import java.util.UUID;

public interface HistoricoStatusRepository {
    List<HistoricoStatus> listarPorOrdemServico(UUID ordemServicoId);
}
