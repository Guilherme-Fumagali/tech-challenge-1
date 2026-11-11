package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.application.port.MetricasOrdemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import com.oficina.mecanica.infrastructure.persistence.entity.HistoricoStatusOSJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.mapper.OrdemServicoMapper;
import com.oficina.mecanica.infrastructure.persistence.repository.OrdemServicoJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrdemServicoRepositoryAdapter implements OrdemServicoRepository {

    static final String EVENTO_ABERTURA = "OrdemServicoAberta";

    private final OrdemServicoJpaRepository jpa;
    private final OrdemServicoMapper mapper;
    private final MetricasOrdemServico metricas;
    private final EntityManager entityManager;

    public OrdemServicoRepositoryAdapter(OrdemServicoJpaRepository jpa,
                                         OrdemServicoMapper mapper,
                                         MetricasOrdemServico metricas,
                                         EntityManager entityManager) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.metricas = metricas;
        this.entityManager = entityManager;
    }

    @Override
    public OrdemServico salvar(OrdemServico os) {
        var salva = jpa.saveAndFlush(mapper.toEntity(os));
        os.registrarVersaoPersistida(salva.getVersao());

        if (os.consumirMarcaDeNovaOrdem()) {
            metricas.registrarAbertura(os.getId());
            entityManager.persist(new HistoricoStatusOSJpaEntity(UUID.randomUUID(), os.getId(), null,
                StatusOS.RECEBIDA, EVENTO_ABERTURA, os.getDataAbertura()));
        }
        for (var transicao : os.drenarTransicoes()) {
            if (transicao.alterouStatus()) {
                metricas.registrarTransicao(transicao);
            }
            entityManager.persist(new HistoricoStatusOSJpaEntity(UUID.randomUUID(), os.getId(), transicao.origem(),
                transicao.destino(), transicao.evento(), transicao.ocorridaEm()));
        }
        return os;
    }

    @Override
    public Optional<OrdemServico> buscarPorId(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existePorId(UUID id) {
        return jpa.existsById(id);
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
