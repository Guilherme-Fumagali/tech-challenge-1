package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.application.port.MetricasOrdemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import com.oficina.mecanica.infrastructure.persistence.mapper.OrdemServicoMapper;
import com.oficina.mecanica.infrastructure.persistence.repository.OrdemServicoJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrdemServicoRepositoryAdapter implements OrdemServicoRepository {

    private final OrdemServicoJpaRepository jpa;
    private final OrdemServicoMapper mapper;
    private final MetricasOrdemServico metricas;

    public OrdemServicoRepositoryAdapter(OrdemServicoJpaRepository jpa,
                                         OrdemServicoMapper mapper,
                                         MetricasOrdemServico metricas) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.metricas = metricas;
    }

    @Override
    public OrdemServico salvar(OrdemServico os) {
        jpa.save(mapper.toEntity(os));

        if (os.consumirMarcaDeNovaOrdem()) {
            metricas.registrarAbertura(os.getId());
        }
        os.drenarTransicoes().forEach(metricas::registrarTransicao);
        return os;
    }

    @Override
    public Optional<OrdemServico> buscarPorId(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<OrdemServico> listarTodas() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> listarAtivasOrdenadas() {
        return jpa.findAtivasOrdenadasPorPrioridade().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> listarPorStatus(StatusOS status) {
        return jpa.findByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> listarPorVeiculo(UUID veiculoId) {
        return jpa.findByVeiculoId(veiculoId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> listarPorCliente(UUID clienteId) {
        return jpa.findByClienteIdOrderByDataAberturaDesc(clienteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> listarFinalizadasNoPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return jpa.findFinalizadasNoPeriodo(inicio, fim).stream().map(mapper::toDomain).toList();
    }
}
