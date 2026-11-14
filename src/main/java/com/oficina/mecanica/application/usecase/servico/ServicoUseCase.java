package com.oficina.mecanica.application.usecase.servico;

import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.Servico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ServicoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ServicoUseCase {

    private final ServicoRepository repository;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public ServicoUseCase(ServicoRepository repository, UnidadeDeTrabalho unidadeDeTrabalho) {
        this.repository = repository;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
    }

    public Servico cadastrar(String nome, String descricao, BigDecimal preco, BigDecimal tempoHoras) {
        var servico = new Servico(UUID.randomUUID(), nome, descricao, preco, tempoHoras);
        return unidadeDeTrabalho.executar(() -> repository.salvar(servico));
    }

    public Servico buscarPorId(UUID id) {
        return repository.buscarPorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
    }

    public List<Servico> listarTodos() {
        return repository.listarTodos();
    }

    public Servico atualizar(UUID id, String nome, String descricao, BigDecimal preco, BigDecimal tempoHoras) {
        return unidadeDeTrabalho.executar(() -> {
            var servico = buscarPorId(id);
            servico.atualizar(nome, descricao, preco, tempoHoras);
            return repository.salvar(servico);
        });
    }

    public void deletar(UUID id) {
        unidadeDeTrabalho.executar(() -> {
            buscarPorId(id);
            repository.deletar(id);
        });
    }
}
