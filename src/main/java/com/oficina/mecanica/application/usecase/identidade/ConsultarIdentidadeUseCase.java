package com.oficina.mecanica.application.usecase.identidade;

import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.IdentidadeNaoEncontradaException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.FuncionarioRepository;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;

public class ConsultarIdentidadeUseCase {

    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;

    public ConsultarIdentidadeUseCase(ClienteRepository clienteRepository,
                                      FuncionarioRepository funcionarioRepository) {
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    public Identidade cliente(String cpfCnpj) {
        return clienteRepository.buscarPorCpfCnpj(new CpfCnpj(cpfCnpj))
            .map(c -> new Identidade(c.getId(), c.getNome(), c.getEmail(), c.getStatus().name()))
            .orElseThrow(IdentidadeNaoEncontradaException::new);
    }

    public Identidade funcionario(String cpf) {
        var documento = new CpfCnpj(cpf);
        if (!documento.isCpf()) {
            throw new DomainException("Funcionário é identificado por CPF.");
        }
        return funcionarioRepository.buscarPorCpf(documento.getValor())
            .map(f -> new Identidade(f.getId(), f.getNome(), f.getEmail(), f.getStatus().name()))
            .orElseThrow(IdentidadeNaoEncontradaException::new);
    }
}
