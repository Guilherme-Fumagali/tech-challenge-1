package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.infrastructure.persistence.entity.ClienteJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.repository.ClienteJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClienteJpaRepository jpa;

    public ClienteRepositoryAdapter(ClienteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        var entity = toEntity(cliente);
        jpa.save(entity);
        return cliente;
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Cliente> buscarPorCpfCnpj(CpfCnpj cpfCnpj) {
        return jpa.findByCpfCnpj(cpfCnpj.getValor()).map(this::toDomain);
    }

    @Override
    public List<Cliente> listarTodos() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deletar(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existePorCpfCnpj(CpfCnpj cpfCnpj) {
        return jpa.existsByCpfCnpj(cpfCnpj.getValor());
    }

    private ClienteJpaEntity toEntity(Cliente c) {
        return new ClienteJpaEntity(c.getId(), c.getCpfCnpj().getValor(),
            c.getNome(), c.getEmail(), c.getTelefone());
    }

    private Cliente toDomain(ClienteJpaEntity e) {
        return new Cliente(e.getId(), new CpfCnpj(e.getCpfCnpj()),
            e.getNome(), e.getEmail(), e.getTelefone());
    }
}
