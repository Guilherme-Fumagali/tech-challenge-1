package com.oficina.mecanica.infrastructure.persistence.adapter;

import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.repository.VeiculoRepository;
import com.oficina.mecanica.domain.valueobject.Placa;
import com.oficina.mecanica.infrastructure.persistence.mapper.VeiculoMapper;
import com.oficina.mecanica.infrastructure.persistence.repository.VeiculoJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class VeiculoRepositoryAdapter implements VeiculoRepository {

    private final VeiculoJpaRepository jpa;
    private final VeiculoMapper mapper;

    public VeiculoRepositoryAdapter(VeiculoJpaRepository jpa, VeiculoMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Veiculo salvar(Veiculo v) {
        jpa.save(mapper.toEntity(v));
        return v;
    }

    @Override
    public Optional<Veiculo> buscarPorId(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Veiculo> buscarPorPlaca(Placa placa) {
        return jpa.findByPlaca(placa.getValor()).map(mapper::toDomain);
    }

    @Override
    public List<Veiculo> listarPorCliente(UUID clienteId) {
        return jpa.findByClienteId(clienteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Veiculo> listarTodos() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deletar(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existePorPlaca(Placa placa) {
        return jpa.existsByPlaca(placa.getValor());
    }
}
