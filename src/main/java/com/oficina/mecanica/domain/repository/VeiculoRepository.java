package com.oficina.mecanica.domain.repository;

import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.valueobject.Placa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoRepository {
    Veiculo salvar(Veiculo veiculo);
    Optional<Veiculo> buscarPorId(UUID id);
    Optional<Veiculo> buscarPorPlaca(Placa placa);
    List<Veiculo> listarPorCliente(UUID clienteId);
    List<Veiculo> listarTodos();
    void deletar(UUID id);
    boolean existePorPlaca(Placa placa);
}
