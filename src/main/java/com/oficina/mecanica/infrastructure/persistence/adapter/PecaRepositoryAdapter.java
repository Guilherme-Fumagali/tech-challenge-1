package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.repository.PecaRepository;
import com.oficina.mecanica.infrastructure.persistence.entity.PecaJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.repository.PecaJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PecaRepositoryAdapter implements PecaRepository {

    private final PecaJpaRepository jpa;

    public PecaRepositoryAdapter(PecaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Peca salvar(Peca p) {
        jpa.save(toEntity(p));
        return p;
    }

    @Override
    public Optional<Peca> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Peca> listarTodos() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Peca> listarAbaixoDoEstoqueMinimo() {
        return jpa.findAbaixoDoEstoqueMinimo().stream().map(this::toDomain).toList();
    }

    @Override
    public void deletar(UUID id) {
        jpa.deleteById(id);
    }

    private PecaJpaEntity toEntity(Peca p) {
        return new PecaJpaEntity(p.getId(), p.getNome(), p.getDescricao(),
            p.getPrecoUnitario(), p.getQuantidadeEstoque(), p.getEstoqueMinimo());
    }

    private Peca toDomain(PecaJpaEntity e) {
        return new Peca(e.getId(), e.getNome(), e.getDescricao(),
            e.getPrecoUnitario(), e.getQuantidadeEstoque(), e.getEstoqueMinimo());
    }
}
