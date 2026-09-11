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
    private LocalDateTime dataAprovacao;
    private LocalDateTime dataInicio;
    private LocalDateTime dataConclusao;
    private boolean excluidaLogicamente;
    private LocalDateTime dataExclusaoLogica;
    private String tokenAprovacaoExterna;
    private LocalDateTime tokenExpiracao;
    private LocalDateTime dataUltimaTransicao;

    @Getter(AccessLevel.NONE)
    private final List<TransicaoOS> transicoesPendentes = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private boolean novaOrdem;

    public OrdemServico(UUID id, UUID clienteId, UUID veiculoId) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.status = StatusOS.RECEBIDA;
        this.itensServico = new ArrayList<>();
        this.itensPeca = new ArrayList<>();
        this.dataAbertura = LocalDateTime.now(ZoneOffset.UTC);
        this.dataUltimaTransicao = this.dataAbertura;
        this.novaOrdem = true;
    }

    public static OrdemServico reconstituir(DadosOrdemServico dados) {
        var os = new OrdemServico(dados.id(), dados.clienteId(), dados.veiculoId());
        os.status = dados.status();
        os.itensServico = dados.itensServico() != null ? new ArrayList<>(dados.itensServico()) : new ArrayList<>();
        os.itensPeca = dados.itensPeca() != null ? new ArrayList<>(dados.itensPeca()) : new ArrayList<>();
        os.dataAbertura = dados.dataAbertura();
        os.dataAprovacao = dados.dataAprovacao();
        os.dataInicio = dados.dataInicio();
        os.dataConclusao = dados.dataConclusao();
        os.excluidaLogicamente = dados.excluidaLogicamente();
        os.dataExclusaoLogica = dados.dataExclusaoLogica();
        os.tokenAprovacaoExterna = dados.tokenAprovacaoExterna();
        os.tokenExpiracao = dados.tokenExpiracao();
        os.dataUltimaTransicao = dados.dataUltimaTransicao() != null
            ? dados.dataUltimaTransicao()
            : dados.dataAbertura();
        os.transicoesPendentes.clear();
        os.novaOrdem = false;
        return os;
    }

    public void iniciarDiagnostico() {
        transicionarPara(StatusOS.EM_DIAGNOSTICO);
    }

    private void transicionarPara(StatusOS destino) {
        status.validarTransicaoPara(destino);
        var agora = LocalDateTime.now(ZoneOffset.UTC);
        var referencia = dataUltimaTransicao != null ? dataUltimaTransicao : dataAbertura;
        var permanencia = referencia != null
            ? Duration.between(referencia, agora)
            : Duration.ZERO;

        transicoesPendentes.add(new TransicaoOS(id, status, destino, permanencia));
        this.status = destino;
        this.dataUltimaTransicao = agora;
    }

    public List<TransicaoOS> drenarTransicoes() {
        var copia = List.copyOf(transicoesPendentes);
        transicoesPendentes.clear();
        return copia;
    }

    public boolean consumirMarcaDeNovaOrdem() {
        var era = novaOrdem;
        novaOrdem = false;
        return era;
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
        transicionarPara(StatusOS.AGUARDANDO_APROVACAO);
        this.tokenAprovacaoExterna = TokenAprovacaoExterna.gerar();
        this.tokenExpiracao = LocalDateTime.now(ZoneOffset.UTC).plus(validadeToken);
    }

    public void aprovar() {
        transicionarPara(StatusOS.EM_EXECUCAO);
        this.dataAprovacao = LocalDateTime.now(ZoneOffset.UTC);
        this.dataInicio = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void reprovar() {
        transicionarPara(StatusOS.CANCELADA);
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
        transicionarPara(StatusOS.FINALIZADA);
        this.dataConclusao = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void entregar() {
        transicionarPara(StatusOS.ENTREGUE);
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
