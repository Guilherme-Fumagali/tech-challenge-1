package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import lombok.Getter;
import lombok.AccessLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class OrdemServico {

    private UUID id;
    private UUID clienteId;
    private UUID veiculoId;
    private StatusOS status;
    @Getter(AccessLevel.NONE) private final List<ItemServico> itensServico;
    @Getter(AccessLevel.NONE) private final List<ItemPeca> itensPeca;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataInicio;
    private LocalDateTime dataConclusao;

    public OrdemServico(UUID id, UUID clienteId, UUID veiculoId) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.status = StatusOS.RECEBIDA;
        this.itensServico = new ArrayList<>();
        this.itensPeca = new ArrayList<>();
        this.dataAbertura = LocalDateTime.now();
    }

    private OrdemServico(UUID id, UUID clienteId, UUID veiculoId, StatusOS status,
                         List<ItemServico> itensServico, List<ItemPeca> itensPeca,
                         LocalDateTime dataAbertura, LocalDateTime dataInicio, LocalDateTime dataConclusao) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.status = status;
        this.itensServico = new ArrayList<>(itensServico);
        this.itensPeca = new ArrayList<>(itensPeca);
        this.dataAbertura = dataAbertura;
        this.dataInicio = dataInicio;
        this.dataConclusao = dataConclusao;
    }

    public static Reconstitucao reconstituir() {
        return new Reconstitucao();
    }

    public static final class Reconstitucao {
        private UUID id, clienteId, veiculoId;
        private StatusOS status;
        private List<ItemServico> itensServico = List.of();
        private List<ItemPeca> itensPeca = List.of();
        private LocalDateTime dataAbertura, dataInicio, dataConclusao;

        private Reconstitucao() {}

        public Reconstitucao id(UUID v)                      { id = v;             return this; }
        public Reconstitucao clienteId(UUID v)               { clienteId = v;      return this; }
        public Reconstitucao veiculoId(UUID v)               { veiculoId = v;      return this; }
        public Reconstitucao status(StatusOS v)              { status = v;         return this; }
        public Reconstitucao itensServico(List<ItemServico> v){ itensServico = v;  return this; }
        public Reconstitucao itensPeca(List<ItemPeca> v)     { itensPeca = v;     return this; }
        public Reconstitucao dataAbertura(LocalDateTime v)   { dataAbertura = v;  return this; }
        public Reconstitucao dataInicio(LocalDateTime v)     { dataInicio = v;    return this; }
        public Reconstitucao dataConclusao(LocalDateTime v)  { dataConclusao = v; return this; }

        public OrdemServico build() {
            return new OrdemServico(id, clienteId, veiculoId, status,
                itensServico, itensPeca, dataAbertura, dataInicio, dataConclusao);
        }
    }

    public void iniciarDiagnostico() {
        status.validarTransicaoPara(StatusOS.EM_DIAGNOSTICO);
        this.status = StatusOS.EM_DIAGNOSTICO;
    }

    public void adicionarServico(ItemServico item) {
        exigirStatus(StatusOS.EM_DIAGNOSTICO, "adicionar serviço");
        itensServico.add(item);
    }

    public void adicionarPeca(ItemPeca item) {
        exigirStatus(StatusOS.EM_DIAGNOSTICO, "adicionar peça");
        itensPeca.add(item);
    }

    public void gerarOrcamento() {
        exigirStatus(StatusOS.EM_DIAGNOSTICO, "gerar orçamento");
        if (itensServico.isEmpty() && itensPeca.isEmpty()) {
            throw new DomainException("A OS deve ter ao menos um item antes de gerar o orçamento.");
        }
        status.validarTransicaoPara(StatusOS.AGUARDANDO_APROVACAO);
        this.status = StatusOS.AGUARDANDO_APROVACAO;
    }

    public void aprovar() {
        status.validarTransicaoPara(StatusOS.EM_EXECUCAO);
        this.status = StatusOS.EM_EXECUCAO;
        this.dataInicio = LocalDateTime.now();
    }

    public void reprovar() {
        status.validarTransicaoPara(StatusOS.CANCELADA);
        this.status = StatusOS.CANCELADA;
    }

    public void concluir() {
        status.validarTransicaoPara(StatusOS.FINALIZADA);
        this.status = StatusOS.FINALIZADA;
        this.dataConclusao = LocalDateTime.now();
    }

    public void entregar() {
        status.validarTransicaoPara(StatusOS.ENTREGUE);
        this.status = StatusOS.ENTREGUE;
    }

    public BigDecimal calcularOrcamento() {
        BigDecimal totalServicos = itensServico.stream()
            .map(ItemServico::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPecas = itensPeca.stream()
            .map(ItemPeca::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalServicos.add(totalPecas);
    }

    private void exigirStatus(StatusOS esperado, String operacao) {
        if (this.status != esperado) {
            throw new DomainException(
                "Operação '%s' não permitida no status %s.".formatted(operacao, this.status));
        }
    }

    public List<ItemServico> getItensServico() { return Collections.unmodifiableList(itensServico); }
    public List<ItemPeca> getItensPeca()       { return Collections.unmodifiableList(itensPeca); }
}
