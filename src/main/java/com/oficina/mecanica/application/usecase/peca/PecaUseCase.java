package com.oficina.mecanica.application.usecase.peca;

import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.PecaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PecaUseCase {

    private final PecaRepository repository;

    public PecaUseCase(PecaRepository repository) {
        this.repository = repository;
    }

    public Peca cadastrar(String nome, String descricao, BigDecimal preco,
                          int quantidadeEstoque, int estoqueMinimo) {
        var peca = new Peca(UUID.randomUUID(), nome, descricao, preco, quantidadeEstoque, estoqueMinimo);
        return repository.salvar(peca);
    }

    public Peca buscarPorId(UUID id) {
        return repository.buscarPorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", id));
    }

    public List<Peca> listarTodas() {
        return repository.listarTodos();
    }

    public List<Peca> listarAbaixoDoEstoqueMinimo() {
        return repository.listarAbaixoDoEstoqueMinimo();
    }

    public Peca atualizar(UUID id, String nome, String descricao, BigDecimal preco, int estoqueMinimo) {
        var peca = buscarPorId(id);
        peca.atualizar(nome, descricao, preco, estoqueMinimo);
        return repository.salvar(peca);
    }

    public Peca incrementarEstoque(UUID id, int quantidade) {
        var peca = buscarPorId(id);
        peca.incrementarEstoque(quantidade);
        return repository.salvar(peca);
    }

    public void deletar(UUID id) {
        buscarPorId(id);
        repository.deletar(id);
    }
}
