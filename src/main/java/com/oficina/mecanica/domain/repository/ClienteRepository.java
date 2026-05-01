package com.oficina.mecanica.domain.repository;

import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository {
    Cliente salvar(Cliente cliente);
    Optional<Cliente> buscarPorId(UUID id);
    Optional<Cliente> buscarPorCpfCnpj(CpfCnpj cpfCnpj);
    List<Cliente> listarTodos();
    void deletar(UUID id);
    boolean existePorCpfCnpj(CpfCnpj cpfCnpj);
}
