package com.oficina.mecanica.domain.repository;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.valueobject.StatusOS;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemServicoRepository {
    OrdemServico salvar(OrdemServico os);
    Optional<OrdemServico> buscarPorId(UUID id);
    List<OrdemServico> listarTodas();
    List<OrdemServico> listarAtivasOrdenadas();
    List<OrdemServico> listarPorStatus(StatusOS status);
    List<OrdemServico> listarPorVeiculo(UUID veiculoId);
    List<OrdemServico> listarFinalizadasNoPeriodo(LocalDateTime inicio, LocalDateTime fim);
}
