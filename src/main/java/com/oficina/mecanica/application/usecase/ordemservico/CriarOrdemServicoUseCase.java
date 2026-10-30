package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.VeiculoRepository;

import java.util.UUID;

public class CriarOrdemServicoUseCase {

    private final OrdemServicoRepository osRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public CriarOrdemServicoUseCase(OrdemServicoRepository osRepository,
                                    ClienteRepository clienteRepository,
                                    VeiculoRepository veiculoRepository,
                                    UnidadeDeTrabalho unidadeDeTrabalho) {
        this.osRepository = osRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
    }

    public OrdemServico executar(UUID clienteId, UUID veiculoId) {
        return unidadeDeTrabalho.executar(() -> {
            var cliente = clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
            var veiculo = veiculoRepository.buscarPorId(veiculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo", veiculoId));

            if (!veiculo.getClienteId().equals(cliente.getId())) {
                throw new com.oficina.mecanica.domain.exception.DomainException(
                    "O veículo não pertence ao cliente informado.");
            }

            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            return osRepository.salvar(os);
        });
    }
}
