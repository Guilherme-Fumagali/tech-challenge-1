package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.ItemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import com.oficina.mecanica.infrastructure.persistence.entity.ItemPecaJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.entity.ItemServicoJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.entity.OrdemServicoJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.repository.OrdemServicoJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrdemServicoRepositoryAdapter implements OrdemServicoRepository {

    private final OrdemServicoJpaRepository jpa;

    public OrdemServicoRepositoryAdapter(OrdemServicoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OrdemServico salvar(OrdemServico os) {
        var entity = toEntity(os);
        jpa.save(entity);
        return os;
    }

    @Override
    public Optional<OrdemServico> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<OrdemServico> listarTodas() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrdemServico> listarPorStatus(StatusOS status) {
        return jpa.findByStatus(status).stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrdemServico> listarPorVeiculo(UUID veiculoId) {
        return jpa.findByVeiculoId(veiculoId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrdemServico> listarFinalizadasNoPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return jpa.findFinalizadasNoPeriodo(inicio, fim).stream().map(this::toDomain).toList();
    }

    private OrdemServicoJpaEntity toEntity(OrdemServico os) {
        var entity = new OrdemServicoJpaEntity();
        entity.setId(os.getId());
        entity.setClienteId(os.getClienteId());
        entity.setVeiculoId(os.getVeiculoId());
        entity.setStatus(os.getStatus());
        entity.setDataAbertura(os.getDataAbertura());
        entity.setDataInicio(os.getDataInicio());
        entity.setDataConclusao(os.getDataConclusao());

        var itensServico = os.getItensServico().stream()
            .map(i -> toItemServicoEntity(i, entity))
            .toList();
        entity.getItensServico().clear();
        entity.getItensServico().addAll(itensServico);

        var itensPeca = os.getItensPeca().stream()
            .map(i -> toItemPecaEntity(i, entity))
            .toList();
        entity.getItensPeca().clear();
        entity.getItensPeca().addAll(itensPeca);

        return entity;
    }

    private ItemServicoJpaEntity toItemServicoEntity(ItemServico i, OrdemServicoJpaEntity os) {
        return new ItemServicoJpaEntity(i.getId(), os, i.getServicoId(),
            i.getNomeServico(), i.getPrecoSnapshot(), i.getQuantidade());
    }

    private ItemPecaJpaEntity toItemPecaEntity(ItemPeca i, OrdemServicoJpaEntity os) {
        return new ItemPecaJpaEntity(i.getId(), os, i.getPecaId(),
            i.getNomePeca(), i.getPrecoSnapshot(), i.getQuantidade());
    }

    private OrdemServico toDomain(OrdemServicoJpaEntity e) {
        var itensServico = e.getItensServico().stream()
            .map(i -> new ItemServico(i.getId(), i.getServicoId(), i.getNomeServico(),
                i.getPrecoSnapshot(), i.getQuantidade()))
            .toList();
        var itensPeca = e.getItensPeca().stream()
            .map(i -> new ItemPeca(i.getId(), i.getPecaId(), i.getNomePeca(),
                i.getPrecoSnapshot(), i.getQuantidade()))
            .toList();
        return new OrdemServico(e.getId(), e.getClienteId(), e.getVeiculoId(),
            e.getStatus(), itensServico, itensPeca,
            e.getDataAbertura(), e.getDataInicio(), e.getDataConclusao());
    }
}
