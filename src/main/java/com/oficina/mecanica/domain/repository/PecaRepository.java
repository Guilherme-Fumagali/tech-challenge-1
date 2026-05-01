package com.oficina.mecanica.domain.repository;

import com.oficina.mecanica.domain.entity.Peca;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepository {
    Peca salvar(Peca peca);
    Optional<Peca> buscarPorId(UUID id);
    List<Peca> listarTodos();
    List<Peca> listarAbaixoDoEstoqueMinimo();
    void deletar(UUID id);
}
