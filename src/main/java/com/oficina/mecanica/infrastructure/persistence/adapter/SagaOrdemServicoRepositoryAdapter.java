package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.repository.SagaOrdemServicoRepository;
import com.oficina.mecanica.domain.saga.PassoSaga;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;
import com.oficina.mecanica.infrastructure.persistence.entity.SagaPassoJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.mapper.SagaOrdemServicoMapper;
import com.oficina.mecanica.infrastructure.persistence.repository.SagaOrdemServicoJpaRepository;
import com.oficina.mecanica.infrastructure.persistence.repository.SagaPassoJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SagaOrdemServicoRepositoryAdapter implements SagaOrdemServicoRepository {

    private static final int TAMANHO_MAXIMO_DETALHE = 500;

    private final SagaOrdemServicoJpaRepository jpa;
    private final SagaPassoJpaRepository passos;
    private final SagaOrdemServicoMapper mapper;
    private final EntityManager entityManager;

    public SagaOrdemServicoRepositoryAdapter(SagaOrdemServicoJpaRepository jpa, SagaPassoJpaRepository passos,
                                             SagaOrdemServicoMapper mapper, EntityManager entityManager) {
        this.jpa = jpa;
        this.passos = passos;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public SagaOrdemServico salvar(SagaOrdemServico saga) {
        var salva = jpa.saveAndFlush(mapper.toEntity(saga));
        saga.registrarVersaoPersistida(salva.getVersao());
        var pendentes = saga.drenarPassos();
        if (!pendentes.isEmpty()) {
            var sequencia = passos.ultimaSequencia(saga.getId());
            for (var passo : pendentes) {
                entityManager.persist(new SagaPassoJpaEntity(UUID.randomUUID(), saga.getId(), ++sequencia,
                    passo.direcao(), passo.tipo(), passo.mensagemId(), passo.resultado(), truncar(passo.detalhe()),
                    mapper.emUtc(passo.ocorridoEm())));
            }
            entityManager.flush();
        }
        return saga;
    }

    @Override
    public Optional<SagaOrdemServico> buscarPorOsId(UUID osId) {
        return jpa.findByOsId(osId).map(mapper::toDomain);
    }

    @Override
    public List<SagaOrdemServico> reivindicarComPrazoVencido(Instant agora, int limite) {
        return jpa.reivindicarComPrazoVencido(mapper.emUtc(agora), limite).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PassoSaga> listarPassos(UUID sagaId) {
        return passos.findBySagaIdOrderBySequencia(sagaId).stream().map(mapper::toDomain).toList();
    }

    private static String truncar(String detalhe) {
        if (detalhe == null || detalhe.length() <= TAMANHO_MAXIMO_DETALHE) {
            return detalhe;
        }
        return detalhe.substring(0, TAMANHO_MAXIMO_DETALHE);
    }
}
