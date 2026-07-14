package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.infrastructure.persistence.mapper.ClienteMapper;
import com.oficina.mecanica.infrastructure.persistence.repository.ClienteJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClienteJpaRepository jpa;
    private final ClienteMapper mapper;

    public ClienteRepositoryAdapter(ClienteJpaRepository jpa, ClienteMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        jpa.save(mapper.toEntity(cliente));
        return cliente;
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Cliente> buscarPorCpfCnpj(CpfCnpj cpfCnpj) {
        return jpa.findByCpfCnpj(cpfCnpj.getValor()).map(mapper::toDomain);
    }

    @Override
    public List<Cliente> listarTodos() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deletar(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existePorCpfCnpj(CpfCnpj cpfCnpj) {
        return jpa.existsByCpfCnpj(cpfCnpj.getValor());
    }
}
