package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.Servico;
import com.oficina.mecanica.domain.repository.ServicoRepository;
import com.oficina.mecanica.infrastructure.persistence.mapper.ServicoMapper;
import com.oficina.mecanica.infrastructure.persistence.repository.ServicoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ServicoRepositoryAdapter implements ServicoRepository {

    private final ServicoJpaRepository jpa;
    private final ServicoMapper mapper;

    public ServicoRepositoryAdapter(ServicoJpaRepository jpa, ServicoMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Servico salvar(Servico s) {
        jpa.save(mapper.toEntity(s));
        return s;
    }

    @Override
    public Optional<Servico> buscarPorId(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Servico> listarTodos() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deletar(UUID id) {
        jpa.deleteById(id);
    }
}
