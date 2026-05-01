package com.oficina.mecanica.domain.repository;

import com.oficina.mecanica.domain.entity.Servico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepository {
    Servico salvar(Servico servico);
    Optional<Servico> buscarPorId(UUID id);
    List<Servico> listarTodos();
    void deletar(UUID id);
}
