package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.Servico;
import com.oficina.mecanica.domain.repository.ServicoRepository;
import com.oficina.mecanica.infrastructure.persistence.entity.ServicoJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.repository.ServicoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ServicoRepositoryAdapter implements ServicoRepository {

    private final ServicoJpaRepository jpa;

    public ServicoRepositoryAdapter(ServicoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Servico salvar(Servico s) {
        jpa.save(toEntity(s));
        return s;
    }

    @Override
    public Optional<Servico> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Servico> listarTodos() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deletar(UUID id) {
        jpa.deleteById(id);
    }

    private ServicoJpaEntity toEntity(Servico s) {
        return new ServicoJpaEntity(s.getId(), s.getNome(), s.getDescricao(),
            s.getPrecoUnitario(), s.getTempoEstimadoHoras());
    }

    private Servico toDomain(ServicoJpaEntity e) {
        return new Servico(e.getId(), e.getNome(), e.getDescricao(),
            e.getPrecoUnitario(), e.getTempoEstimadoHoras());
    }
}
