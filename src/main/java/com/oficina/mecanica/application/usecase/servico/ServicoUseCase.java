package com.oficina.mecanica.application.usecase.servico;

import com.oficina.mecanica.domain.entity.Servico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ServicoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ServicoUseCase {

    private final ServicoRepository repository;

    public ServicoUseCase(ServicoRepository repository) {
        this.repository = repository;
    }

    public Servico cadastrar(String nome, String descricao, BigDecimal preco, BigDecimal tempoHoras) {
        var servico = new Servico(UUID.randomUUID(), nome, descricao, preco, tempoHoras);
        return repository.salvar(servico);
    }

    public Servico buscarPorId(UUID id) {
        return repository.buscarPorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
    }

    public List<Servico> listarTodos() {
        return repository.listarTodos();
    }

    public Servico atualizar(UUID id, String nome, String descricao, BigDecimal preco, BigDecimal tempoHoras) {
        var servico = buscarPorId(id);
        servico.atualizar(nome, descricao, preco, tempoHoras);
        return repository.salvar(servico);
    }

    public void deletar(UUID id) {
        buscarPorId(id);
        repository.deletar(id);
    }
}
