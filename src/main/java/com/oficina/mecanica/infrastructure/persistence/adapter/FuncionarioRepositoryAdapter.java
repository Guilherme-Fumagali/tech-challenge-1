package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.Funcionario;
import com.oficina.mecanica.domain.repository.FuncionarioRepository;
import com.oficina.mecanica.infrastructure.persistence.mapper.FuncionarioMapper;
import com.oficina.mecanica.infrastructure.persistence.repository.FuncionarioJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FuncionarioRepositoryAdapter implements FuncionarioRepository {

    private final FuncionarioJpaRepository jpa;
    private final FuncionarioMapper mapper;

    public FuncionarioRepositoryAdapter(FuncionarioJpaRepository jpa, FuncionarioMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<Funcionario> buscarPorCpf(String cpf) {
        return jpa.findByCpf(cpf).map(mapper::toDomain);
    }
}
