package com.oficina.mecanica.application.usecase.veiculo;

import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.VeiculoRepository;
import com.oficina.mecanica.domain.valueobject.Placa;

import java.util.List;
import java.util.UUID;

public class VeiculoUseCase {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public VeiculoUseCase(VeiculoRepository veiculoRepository, ClienteRepository clienteRepository,
                          UnidadeDeTrabalho unidadeDeTrabalho) {
        this.veiculoRepository = veiculoRepository;
        this.clienteRepository = clienteRepository;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
    }

    public Veiculo cadastrar(Placa placa, String marca, String modelo, int anoFabricacao, UUID clienteId) {
        return unidadeDeTrabalho.executar(() -> {
            clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
            if (veiculoRepository.existePorPlaca(placa)) {
                throw new DomainException("Já existe um veículo com a placa: " + placa.getValor());
            }
            var veiculo = new Veiculo(UUID.randomUUID(), placa, marca, modelo, anoFabricacao, clienteId);
            return veiculoRepository.salvar(veiculo);
        });
    }

    public Veiculo buscarPorId(UUID id) {
        return veiculoRepository.buscarPorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo", id));
    }

    public Veiculo buscarPorIdDoCliente(UUID id, UUID clienteId) {
        var veiculo = buscarPorId(id);
        if (!veiculo.getClienteId().equals(clienteId)) {
            throw new RecursoNaoEncontradoException("Veículo", id);
        }
        return veiculo;
    }

    public List<Veiculo> listarTodos() {
        return veiculoRepository.listarTodos();
    }

    public List<Veiculo> listarPorCliente(UUID clienteId) {
        return veiculoRepository.listarPorCliente(clienteId);
    }

    public Veiculo atualizar(UUID id, String marca, String modelo, int anoFabricacao) {
        return unidadeDeTrabalho.executar(() -> {
            var veiculo = buscarPorId(id);
            veiculo.atualizar(marca, modelo, anoFabricacao);
            return veiculoRepository.salvar(veiculo);
        });
    }

    public void deletar(UUID id) {
        unidadeDeTrabalho.executar(() -> {
            buscarPorId(id);
            veiculoRepository.deletar(id);
        });
    }
}
