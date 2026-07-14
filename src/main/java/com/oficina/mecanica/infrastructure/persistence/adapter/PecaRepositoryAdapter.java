package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.repository.PecaRepository;
import com.oficina.mecanica.infrastructure.persistence.mapper.PecaMapper;
import com.oficina.mecanica.infrastructure.persistence.repository.PecaJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PecaRepositoryAdapter implements PecaRepository {

    private final PecaJpaRepository jpa;
    private final PecaMapper mapper;

    public PecaRepositoryAdapter(PecaJpaRepository jpa, PecaMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Peca salvar(Peca p) {
        jpa.save(mapper.toEntity(p));
        return p;
    }

    @Override
    public Optional<Peca> buscarPorId(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Peca> listarTodos() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Peca> listarAbaixoDoEstoqueMinimo() {
        return jpa.findAbaixoDoEstoqueMinimo().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deletar(UUID id) {
        jpa.deleteById(id);
    }
}
