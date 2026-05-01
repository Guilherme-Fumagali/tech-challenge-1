package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.repository.VeiculoRepository;
import com.oficina.mecanica.domain.valueobject.Placa;
import com.oficina.mecanica.infrastructure.persistence.entity.VeiculoJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.repository.VeiculoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class VeiculoRepositoryAdapter implements VeiculoRepository {

    private final VeiculoJpaRepository jpa;

    public VeiculoRepositoryAdapter(VeiculoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Veiculo salvar(Veiculo v) {
        jpa.save(toEntity(v));
        return v;
    }

    @Override
    public Optional<Veiculo> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Veiculo> buscarPorPlaca(Placa placa) {
        return jpa.findByPlaca(placa.getValor()).map(this::toDomain);
    }

    @Override
    public List<Veiculo> listarPorCliente(UUID clienteId) {
        return jpa.findByClienteId(clienteId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Veiculo> listarTodos() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deletar(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existePorPlaca(Placa placa) {
        return jpa.existsByPlaca(placa.getValor());
    }

    private VeiculoJpaEntity toEntity(Veiculo v) {
        return new VeiculoJpaEntity(v.getId(), v.getPlaca().getValor(),
            v.getMarca(), v.getModelo(), v.getAnoFabricacao(), v.getClienteId());
    }

    private Veiculo toDomain(VeiculoJpaEntity e) {
        return new Veiculo(e.getId(), new Placa(e.getPlaca()),
            e.getMarca(), e.getModelo(), e.getAnoFabricacao(), e.getClienteId());
    }
}
