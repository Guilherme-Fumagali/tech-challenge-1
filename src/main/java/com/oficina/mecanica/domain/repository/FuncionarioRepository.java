package com.oficina.mecanica.domain.repository;

import com.oficina.mecanica.domain.entity.Funcionario;

import java.util.Optional;

public interface FuncionarioRepository {
    Optional<Funcionario> buscarPorCpf(String cpf);
}
