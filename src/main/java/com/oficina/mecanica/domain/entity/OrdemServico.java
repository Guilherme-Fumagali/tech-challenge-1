package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import lombok.AccessLevel;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class OrdemServico {

    static final String EVENTO_DIAGNOSTICO_INICIADO = "DiagnosticoIniciado";
    static final String EVENTO_ORCAMENTO_GERADO = "OrcamentoGerado";
    static final String EVENTO_ORCAMENTO_APROVADO = "OrcamentoAprovado";
    static final String EVENTO_PAGAMENTO_APROVADO = "PagamentoAprovado";
    static final String EVENTO_EXECUCAO_FINALIZADA = "ExecucaoFinalizada";
    static final String EVENTO_VEICULO_ENTREGUE = "VeiculoEntregue";

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
    private LocalDateTime dataUltimaTransicao;
    private Long versao;

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
        os.dataUltimaTransicao = dados.dataUltimaTransicao() != null
            ? dados.dataUltimaTransicao()
            : dados.dataAbertura();
        os.versao = dados.versao();
        os.transicoesPendentes.clear();
        os.novaOrdem = false;
        return os;
    }

    public void registrarInicioDiagnostico() {
        transicionarPara(StatusOS.EM_DIAGNOSTICO, EVENTO_DIAGNOSTICO_INICIADO);
    }

    public void registrarOrcamentoEnviado() {
        exigirStatus(StatusOS.EM_DIAGNOSTICO, "registrar orçamento enviado");
        if (!possuiItens()) {
            throw new DomainException("A OS deve ter ao menos um item antes de gerar o orçamento.");
        }
        transicionarPara(StatusOS.AGUARDANDO_APROVACAO, EVENTO_ORCAMENTO_GERADO);
    }

    public void registrarAprovacao(Instant aprovadaEm) {
        transicionarPara(StatusOS.AGUARDANDO_PAGAMENTO, EVENTO_ORCAMENTO_APROVADO);
        this.dataAprovacao = emUtc(aprovadaEm);
    }

    public void registrarPagamento(Instant pagoEm) {
        transicionarPara(StatusOS.EM_EXECUCAO, EVENTO_PAGAMENTO_APROVADO);
        this.dataInicio = emUtc(pagoEm);
    }

    public void registrarConclusao(Instant concluidaEm) {
        transicionarPara(StatusOS.FINALIZADA, EVENTO_EXECUCAO_FINALIZADA);
        this.dataConclusao = emUtc(concluidaEm);
    }

    public void cancelar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new DomainException("O cancelamento da OS exige um motivo.");
        }
        transicionarPara(StatusOS.CANCELADA, motivo);
    }

    public void entregar() {
        transicionarPara(StatusOS.ENTREGUE, EVENTO_VEICULO_ENTREGUE);
    }

    public void adicionarServico(ItemServico item) {
        exigirStatus(StatusOS.EM_DIAGNOSTICO, "adicionar serviço");
        itensServico.add(item);
    }

    public void adicionarPeca(ItemPeca item) {
        exigirStatus(StatusOS.EM_DIAGNOSTICO, "adicionar peça");
        itensPeca.add(item);
    }

    public boolean possuiItens() {
        return !itensServico.isEmpty() || !itensPeca.isEmpty();
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

    public List<TransicaoOS> drenarTransicoes() {
        var copia = List.copyOf(transicoesPendentes);
        transicoesPendentes.clear();
        return copia;
    }

    public void registrarVersaoPersistida(Long versaoPersistida) {
        this.versao = versaoPersistida;
    }

    public boolean consumirMarcaDeNovaOrdem() {
        var era = novaOrdem;
        novaOrdem = false;
        return era;
    }

    public List<ItemServico> getItensServico() { return Collections.unmodifiableList(itensServico); }
    public List<ItemPeca> getItensPeca()       { return Collections.unmodifiableList(itensPeca); }

    private void transicionarPara(StatusOS destino, String evento) {
        status.validarTransicaoPara(destino);
        var agora = LocalDateTime.now(ZoneOffset.UTC);
        var referencia = dataUltimaTransicao != null ? dataUltimaTransicao : dataAbertura;
        var permanencia = referencia != null
            ? Duration.between(referencia.atOffset(ZoneOffset.UTC), agora.atOffset(ZoneOffset.UTC))
            : Duration.ZERO;

        transicoesPendentes.add(new TransicaoOS(id, status, destino, permanencia, evento, agora));
        this.status = destino;
        this.dataUltimaTransicao = agora;
    }

    private void exigirStatus(StatusOS esperado, String operacao) {
        if (this.status != esperado) {
            throw new DomainException(
                "Operação '%s' não permitida no status %s.".formatted(operacao, this.status));
        }
    }

    private static LocalDateTime emUtc(Instant instante) {
        return LocalDateTime.ofInstant(instante != null ? instante : Instant.now(), ZoneOffset.UTC);
    }
}
