package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.TokenAprovacaoInvalidoException;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import com.oficina.mecanica.domain.valueobject.TokenAprovacaoExterna;
import lombok.Getter;
import lombok.AccessLevel;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class OrdemServico {

    private static final Duration VALIDADE_TOKEN_PADRAO = Duration.ofHours(168);

    private UUID id;
    private UUID clienteId;
    private UUID veiculoId;
    private StatusOS status;
    @Getter(AccessLevel.NONE) private List<ItemServico> itensServico;
    @Getter(AccessLevel.NONE) private List<ItemPeca> itensPeca;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataInicio;
    private LocalDateTime dataConclusao;
    private boolean excluidaLogicamente;
    private LocalDateTime dataExclusaoLogica;
    private String tokenAprovacaoExterna;
    private LocalDateTime tokenExpiracao;

    public OrdemServico(UUID id, UUID clienteId, UUID veiculoId) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.status = StatusOS.RECEBIDA;
        this.itensServico = new ArrayList<>();
        this.itensPeca = new ArrayList<>();
        this.dataAbertura = LocalDateTime.now(ZoneOffset.UTC);
    }

    public static OrdemServico reconstituir(DadosOrdemServico dados) {
        var os = new OrdemServico(dados.id(), dados.clienteId(), dados.veiculoId());
        os.status = dados.status();
        os.itensServico = dados.itensServico() != null ? new ArrayList<>(dados.itensServico()) : new ArrayList<>();
        os.itensPeca = dados.itensPeca() != null ? new ArrayList<>(dados.itensPeca()) : new ArrayList<>();
        os.dataAbertura = dados.dataAbertura();
        os.dataInicio = dados.dataInicio();
        os.dataConclusao = dados.dataConclusao();
        os.excluidaLogicamente = dados.excluidaLogicamente();
        os.dataExclusaoLogica = dados.dataExclusaoLogica();
        os.tokenAprovacaoExterna = dados.tokenAprovacaoExterna();
        os.tokenExpiracao = dados.tokenExpiracao();
        return os;
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
        gerarOrcamento(VALIDADE_TOKEN_PADRAO);
    }

    public void gerarOrcamento(Duration validadeToken) {
        exigirStatus(StatusOS.EM_DIAGNOSTICO, "gerar orçamento");
        if (itensServico.isEmpty() && itensPeca.isEmpty()) {
            throw new DomainException("A OS deve ter ao menos um item antes de gerar o orçamento.");
        }
        status.validarTransicaoPara(StatusOS.AGUARDANDO_APROVACAO);
        this.status = StatusOS.AGUARDANDO_APROVACAO;
        this.tokenAprovacaoExterna = TokenAprovacaoExterna.gerar();
        this.tokenExpiracao = LocalDateTime.now(ZoneOffset.UTC).plus(validadeToken);
    }

    public void aprovar() {
        status.validarTransicaoPara(StatusOS.EM_EXECUCAO);
        this.status = StatusOS.EM_EXECUCAO;
        this.dataInicio = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void reprovar() {
        status.validarTransicaoPara(StatusOS.CANCELADA);
        this.status = StatusOS.CANCELADA;
    }

    public void aprovarViaTokenExterno(String token) {
        validarTokenAprovacao(token);
        aprovar();
        invalidarTokenAprovacao();
    }

    public void reprovarViaTokenExterno(String token) {
        validarTokenAprovacao(token);
        reprovar();
        invalidarTokenAprovacao();
    }

    private void validarTokenAprovacao(String token) {
        if (tokenAprovacaoExterna == null || !tokenAprovacaoExterna.equals(token)) {
            throw new TokenAprovacaoInvalidoException("Token de aprovação inválido.");
        }
        if (tokenExpiracao == null || tokenExpiracao.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new TokenAprovacaoInvalidoException("Token de aprovação expirado.");
        }
    }

    private void invalidarTokenAprovacao() {
        this.tokenAprovacaoExterna = null;
        this.tokenExpiracao = null;
    }

    public void concluir() {
        status.validarTransicaoPara(StatusOS.FINALIZADA);
        this.status = StatusOS.FINALIZADA;
        this.dataConclusao = LocalDateTime.now(ZoneOffset.UTC);
        this.excluidaLogicamente = true;
        this.dataExclusaoLogica = LocalDateTime.now(ZoneOffset.UTC);
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
