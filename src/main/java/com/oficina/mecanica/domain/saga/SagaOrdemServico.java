package com.oficina.mecanica.domain.saga;

import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.EventoForaDeOrdemException;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.oficina.mecanica.domain.saga.EstadoSaga.*;

@Getter
public class SagaOrdemServico {

    static final String MOTIVO_REABERTURA = "Diagnóstico concluído sem serviços ou peças na ordem de serviço.";
    static final String PASSO_PRAZO_ESGOTADO = "PrazoEsgotado";

    private final UUID id;
    private final UUID osId;
    private EstadoSaga estado;
    private MotivoCompensacao motivoCompensacao;
    @Getter(AccessLevel.NONE)
    private final List<Compensacao> compensacoesPendentes = new ArrayList<>();
    private TipoComando ultimoComandoTipo;
    @Getter(AccessLevel.NONE)
    private Map<String, Object> ultimoComandoDados;
    private Instant prazoResposta;
    private int tentativasReenvio;
    private final Instant criadaEm;
    private Instant atualizadaEm;
    private Long versao;

    @Getter(AccessLevel.NONE)
    private final List<PassoSaga> passosPendentes = new ArrayList<>();

    private SagaOrdemServico(UUID id, UUID osId, EstadoSaga estado, Instant criadaEm) {
        this.id = id;
        this.osId = osId;
        this.estado = estado;
        this.criadaEm = criadaEm;
        this.atualizadaEm = criadaEm;
    }

    public static SagaOrdemServico iniciar(UUID id, UUID osId, Instant agora) {
        return new SagaOrdemServico(id, osId, AGUARDANDO_FILA_DIAGNOSTICO, agora);
    }

    public static SagaOrdemServico reconstituir(DadosSagaOrdemServico dados) {
        var saga = new SagaOrdemServico(dados.id(), dados.osId(), dados.estado(), dados.criadaEm());
        saga.motivoCompensacao = dados.motivoCompensacao();
        if (dados.compensacoesPendentes() != null) {
            saga.compensacoesPendentes.addAll(dados.compensacoesPendentes());
        }
        saga.ultimoComandoTipo = dados.ultimoComandoTipo();
        saga.ultimoComandoDados = dados.ultimoComandoDados() != null
            ? new LinkedHashMap<>(dados.ultimoComandoDados())
            : null;
        saga.prazoResposta = dados.prazoResposta();
        saga.tentativasReenvio = dados.tentativasReenvio();
        saga.atualizadaEm = dados.atualizadaEm() != null ? dados.atualizadaEm() : dados.criadaEm();
        saga.versao = dados.versao();
        return saga;
    }

    public DecisaoSaga aplicar(EventoSaga evento, boolean osPossuiItens, Instant agora) {
        var estadoAnterior = estado;
        var atualizadaAntes = atualizadaEm;
        var efeito = switch (estado) {
            case COMPENSANDO -> aplicarDuranteCompensacao(evento);
            case CONCLUIDA, COMPENSADA, FALHA -> Efeito.ignorado("saga encerrada em " + estado);
            default -> aplicarNoFluxo(evento, osPossuiItens);
        };
        var detalheDoPasso = efeito.detalhe != null ? efeito.detalhe : evento.detalhe();
        passosPendentes.add(passoRecebido(evento, efeito, detalheDoPasso, agora));
        if (efeito.compensacaoLocal != null) {
            passosPendentes.add(new PassoSaga(DirecaoPasso.LOCAL, efeito.compensacaoLocal.name(), null,
                ResultadoPasso.APLICADO, motivoCompensacao.name(), agora));
        }
        if (efeito.resultado == ResultadoPasso.APLICADO) {
            atualizadaEm = agora;
        }
        var permanencia = estado != estadoAnterior ? Duration.between(atualizadaAntes, agora) : null;
        return new DecisaoSaga(efeito.resultado, estadoAnterior, estado, permanencia, efeito.acao,
            List.copyOf(efeito.comandos), efeito.compensacaoLocal, detalheDoPasso);
    }

    public void registrarEnvio(UUID mensagemId, TipoComando comando, Map<String, Object> dados,
                               Instant agora, Duration prazo) {
        passosPendentes.add(new PassoSaga(DirecaoPasso.ENVIADO, comando.nome(), mensagemId,
            ResultadoPasso.APLICADO, null, agora));
        if (comando.exigeResposta() && !estado.encerrado()) {
            ultimoComandoTipo = comando;
            ultimoComandoDados = new LinkedHashMap<>(dados);
            prazoResposta = agora.plus(prazo);
            tentativasReenvio = 0;
        }
        atualizadaEm = agora;
    }

    public boolean prazoVencido(Instant agora) {
        return prazoResposta != null && prazoResposta.isBefore(agora);
    }

    public boolean reenviosEsgotados(int maximoDeReenvios) {
        return tentativasReenvio >= maximoDeReenvios;
    }

    public void registrarReenvio(UUID mensagemId, Instant agora, Duration prazo) {
        if (ultimoComandoTipo == null || prazoResposta == null) {
            throw new DomainException("A saga %s não aguarda resposta de comando.".formatted(id));
        }
        tentativasReenvio++;
        prazoResposta = agora.plus(prazo);
        atualizadaEm = agora;
        passosPendentes.add(new PassoSaga(DirecaoPasso.ENVIADO, ultimoComandoTipo.nome(), mensagemId,
            ResultadoPasso.APLICADO, "reenvio %d".formatted(tentativasReenvio), agora));
    }

    public EstadoSaga registrarFalhaPorPrazo(Instant agora) {
        var estadoAnterior = estado;
        var comando = ultimoComandoTipo != null ? ultimoComandoTipo.nome() : "comando";
        estado = FALHA;
        prazoResposta = null;
        atualizadaEm = agora;
        passosPendentes.add(new PassoSaga(DirecaoPasso.LOCAL, PASSO_PRAZO_ESGOTADO, null, ResultadoPasso.APLICADO,
            "%s sem resposta após %d reenvios".formatted(comando, tentativasReenvio), agora));
        return estadoAnterior;
    }

    public void registrarEventoRejeitado(String tipo, UUID mensagemId, String detalhe, Instant agora) {
        passosPendentes.add(new PassoSaga(DirecaoPasso.RECEBIDO, tipo, mensagemId, ResultadoPasso.REJEITADO,
            detalhe, agora));
    }

    public List<Compensacao> getCompensacoesPendentes() {
        return Collections.unmodifiableList(compensacoesPendentes);
    }

    public Map<String, Object> getUltimoComandoDados() {
        return ultimoComandoDados == null ? Map.of() : Collections.unmodifiableMap(ultimoComandoDados);
    }

    public List<PassoSaga> drenarPassos() {
        var copia = List.copyOf(passosPendentes);
        passosPendentes.clear();
        return copia;
    }

    public void registrarVersaoPersistida(Long versaoPersistida) {
        this.versao = versaoPersistida;
    }

    private Efeito aplicarNoFluxo(EventoSaga evento, boolean osPossuiItens) {
        if (evento.tipo() == TipoEvento.COMANDO_REJEITADO) {
            return aplicarRejeicao(evento);
        }
        if (evento.tipo() == TipoEvento.EXECUCAO_ENFILEIRADA && evento.etapa() == null) {
            return Efeito.ignorado("etapa da execução não informada");
        }
        var faixa = faixaEsperada(evento);
        if (faixa == null) {
            return Efeito.ignorado("evento sem efeito fora de compensação");
        }
        var posicao = estado.posicaoNoFluxo();
        if (posicao < faixa[0]) {
            throw new EventoForaDeOrdemException(evento.tipo().nome(), estado.name());
        }
        if (posicao > faixa[1]) {
            return Efeito.ignorado("evento já refletido no estado " + estado);
        }
        return switch (evento.tipo()) {
            case EXECUCAO_ENFILEIRADA -> evento.etapa() == EtapaExecucao.DIAGNOSTICO
                ? transicionar(AGUARDANDO_DIAGNOSTICO, AcaoOrdemServico.NENHUMA)
                : transicionar(EM_EXECUCAO, AcaoOrdemServico.NENHUMA);
            case DIAGNOSTICO_INICIADO -> transicionar(EM_DIAGNOSTICO, AcaoOrdemServico.REGISTRAR_INICIO_DIAGNOSTICO);
            case DIAGNOSTICO_CONCLUIDO -> osPossuiItens
                ? transicionar(AGUARDANDO_ORCAMENTO, AcaoOrdemServico.NENHUMA)
                    .comComando(new ComandoSaga(TipoComando.GERAR_ORCAMENTO, null))
                : transicionar(EM_DIAGNOSTICO, AcaoOrdemServico.NENHUMA)
                    .comComando(new ComandoSaga(TipoComando.REABRIR_DIAGNOSTICO, MOTIVO_REABERTURA));
            case ORCAMENTO_GERADO -> transicionar(AGUARDANDO_APROVACAO, AcaoOrdemServico.REGISTRAR_ORCAMENTO_ENVIADO);
            case ORCAMENTO_APROVADO -> transicionar(AGUARDANDO_PAGAMENTO, AcaoOrdemServico.REGISTRAR_APROVACAO);
            case ORCAMENTO_REPROVADO, ORCAMENTO_EXPIRADO -> iniciarCompensacao(MotivoCompensacao.REPROVACAO);
            case PAGAMENTO_APROVADO -> transicionar(AGUARDANDO_FILA_REPARO, AcaoOrdemServico.REGISTRAR_PAGAMENTO)
                .comComando(new ComandoSaga(TipoComando.INICIAR_REPARO, null));
            case PAGAMENTO_RECUSADO -> iniciarCompensacao(MotivoCompensacao.PAGAMENTO_RECUSADO);
            case REPARO_INICIADO -> transicionar(EM_EXECUCAO, AcaoOrdemServico.REGISTRAR_INICIO_REPARO);
            case EXECUCAO_FINALIZADA -> transicionar(CONCLUIDA, AcaoOrdemServico.REGISTRAR_CONCLUSAO);
            case EXECUCAO_FALHOU -> iniciarCompensacao(MotivoCompensacao.FALHA_EXECUCAO);
            default -> Efeito.ignorado("evento sem efeito no fluxo");
        };
    }

    private Efeito aplicarRejeicao(EventoSaga evento) {
        var aguardado = switch (estado) {
            case AGUARDANDO_FILA_DIAGNOSTICO -> TipoComando.INICIAR_DIAGNOSTICO;
            case AGUARDANDO_ORCAMENTO -> TipoComando.GERAR_ORCAMENTO;
            case AGUARDANDO_FILA_REPARO -> TipoComando.INICIAR_REPARO;
            default -> null;
        };
        if (aguardado == null || !aguardado.nome().equals(evento.comandoRejeitado())) {
            return Efeito.ignorado("rejeição de comando que a saga não aguarda");
        }
        if (aguardado == TipoComando.GERAR_ORCAMENTO) {
            return iniciarCompensacao(MotivoCompensacao.REJEICAO);
        }
        return falhar("%s rejeitado: %s".formatted(aguardado.nome(), evento.detalhe()));
    }

    private Efeito aplicarDuranteCompensacao(EventoSaga evento) {
        if (compensacoesPendentes.isEmpty()) {
            return Efeito.ignorado("nenhuma compensação pendente");
        }
        var atual = compensacoesPendentes.get(0);
        if (evento.tipo() == atual.respostaQueLibera()) {
            compensacoesPendentes.remove(0);
            return avancarCompensacao();
        }
        if (evento.tipo() == TipoEvento.COMANDO_REJEITADO && atual.comando().nome().equals(evento.comandoRejeitado())) {
            return falhar("compensação %s rejeitada: %s".formatted(atual.name(), evento.detalhe()));
        }
        return Efeito.ignorado("compensação %s aguarda %s".formatted(atual.name(), atual.respostaQueLibera().nome()));
    }

    private Efeito iniciarCompensacao(MotivoCompensacao motivo) {
        motivoCompensacao = motivo;
        compensacoesPendentes.clear();
        compensacoesPendentes.addAll(motivo.compensacoes());
        estado = COMPENSANDO;
        prazoResposta = null;
        return avancarCompensacao();
    }

    private Efeito avancarCompensacao() {
        var efeito = Efeito.aplicado(AcaoOrdemServico.NENHUMA);
        while (!compensacoesPendentes.isEmpty()) {
            var proxima = compensacoesPendentes.get(0);
            if (!proxima.local()) {
                efeito.comComando(new ComandoSaga(proxima.comando(), motivoCompensacao.name()));
                break;
            }
            compensacoesPendentes.remove(0);
            efeito.compensacaoLocal = proxima;
        }
        prazoResposta = null;
        if (compensacoesPendentes.isEmpty()) {
            estado = COMPENSADA;
        }
        return efeito;
    }

    private Efeito transicionar(EstadoSaga destino, AcaoOrdemServico acao) {
        estado = destino;
        prazoResposta = null;
        return Efeito.aplicado(acao);
    }

    private Efeito falhar(String detalhe) {
        estado = FALHA;
        prazoResposta = null;
        var efeito = Efeito.aplicado(AcaoOrdemServico.NENHUMA);
        efeito.detalhe = detalhe;
        return efeito;
    }

    private static int[] faixaEsperada(EventoSaga evento) {
        return switch (evento.tipo()) {
            case EXECUCAO_ENFILEIRADA -> evento.etapa() == EtapaExecucao.DIAGNOSTICO
                ? faixa(AGUARDANDO_FILA_DIAGNOSTICO, AGUARDANDO_FILA_DIAGNOSTICO)
                : faixa(AGUARDANDO_FILA_REPARO, AGUARDANDO_FILA_REPARO);
            case DIAGNOSTICO_INICIADO -> faixa(AGUARDANDO_FILA_DIAGNOSTICO, AGUARDANDO_DIAGNOSTICO);
            case DIAGNOSTICO_CONCLUIDO -> faixa(EM_DIAGNOSTICO, EM_DIAGNOSTICO);
            case ORCAMENTO_GERADO -> faixa(AGUARDANDO_ORCAMENTO, AGUARDANDO_ORCAMENTO);
            case ORCAMENTO_APROVADO, ORCAMENTO_REPROVADO, ORCAMENTO_EXPIRADO ->
                faixa(AGUARDANDO_APROVACAO, AGUARDANDO_APROVACAO);
            case PAGAMENTO_APROVADO, PAGAMENTO_RECUSADO -> faixa(AGUARDANDO_PAGAMENTO, AGUARDANDO_PAGAMENTO);
            case REPARO_INICIADO, EXECUCAO_FALHOU -> faixa(AGUARDANDO_FILA_REPARO, EM_EXECUCAO);
            case EXECUCAO_FINALIZADA -> faixa(EM_EXECUCAO, EM_EXECUCAO);
            default -> null;
        };
    }

    private static int[] faixa(EstadoSaga primeiro, EstadoSaga ultimo) {
        return new int[] {primeiro.posicaoNoFluxo(), ultimo.posicaoNoFluxo()};
    }

    private static PassoSaga passoRecebido(EventoSaga evento, Efeito efeito, String detalhe, Instant agora) {
        return new PassoSaga(DirecaoPasso.RECEBIDO, evento.tipo().nome(), evento.mensagemId(), efeito.resultado,
            detalhe, agora);
    }

    private static final class Efeito {
        private final ResultadoPasso resultado;
        private final AcaoOrdemServico acao;
        private final List<ComandoSaga> comandos = new ArrayList<>();
        private Compensacao compensacaoLocal;
        private String detalhe;

        private Efeito(ResultadoPasso resultado, AcaoOrdemServico acao) {
            this.resultado = resultado;
            this.acao = acao;
        }

        static Efeito aplicado(AcaoOrdemServico acao) {
            return new Efeito(ResultadoPasso.APLICADO, acao);
        }

        static Efeito ignorado(String detalhe) {
            var efeito = new Efeito(ResultadoPasso.IGNORADO, AcaoOrdemServico.NENHUMA);
            efeito.detalhe = detalhe;
            return efeito;
        }

        Efeito comComando(ComandoSaga comando) {
            comandos.add(comando);
            return this;
        }
    }
}
