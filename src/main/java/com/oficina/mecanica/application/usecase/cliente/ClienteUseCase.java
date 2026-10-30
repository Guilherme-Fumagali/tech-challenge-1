package com.oficina.mecanica.application.usecase.cliente;

import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;

import java.util.List;
import java.util.UUID;

public class ClienteUseCase {

    private final ClienteRepository repository;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public ClienteUseCase(ClienteRepository repository, UnidadeDeTrabalho unidadeDeTrabalho) {
        this.repository = repository;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
    }

    public Cliente cadastrar(CpfCnpj cpfCnpj, String nome, String email, String telefone) {
        return unidadeDeTrabalho.executar(() -> {
            if (repository.existePorCpfCnpj(cpfCnpj)) {
                throw new DomainException("Já existe um cliente com o CPF/CNPJ informado.");
            }
            var cliente = new Cliente(UUID.randomUUID(), cpfCnpj, nome, email, telefone);
            return repository.salvar(cliente);
        });
    }

    public Cliente buscarPorId(UUID id) {
        return repository.buscarPorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    public Cliente buscarPorCpfCnpj(String cpfCnpj) {
        return repository.buscarPorCpfCnpj(new CpfCnpj(cpfCnpj))
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", cpfCnpj));
    }

    public List<Cliente> listarTodos() {
        return repository.listarTodos();
    }

    public Cliente atualizar(UUID id, String nome, String email, String telefone) {
        return unidadeDeTrabalho.executar(() -> {
            var cliente = buscarPorId(id);
            cliente.atualizar(nome, email, telefone);
            return repository.salvar(cliente);
        });
    }

    public void deletar(UUID id) {
        unidadeDeTrabalho.executar(() -> {
            buscarPorId(id);
            repository.deletar(id);
        });
    }
}
