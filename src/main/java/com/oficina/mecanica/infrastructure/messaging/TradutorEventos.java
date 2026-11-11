package com.oficina.mecanica.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.oficina.mecanica.domain.saga.EtapaExecucao;
import com.oficina.mecanica.domain.saga.EventoSaga;
import com.oficina.mecanica.domain.saga.TipoEvento;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@Component
public class TradutorEventos {

    private static final int TAMANHO_MAXIMO_DETALHE = 500;

    public EventoSaga traduzir(MensagemEnvelope envelope, TipoEvento tipo) {
        var dados = envelope.dados();
        return EventoSaga.builder()
            .mensagemId(envelope.mensagemId())
            .tipo(tipo)
            .ocorridoEm(envelope.ocorridoEm())
            .instanteDoFato(instante(dados, campoDoInstante(tipo)))
            .etapa(etapa(dados))
            .comandoRejeitado(texto(dados, "comando"))
            .detalhe(detalhe(tipo, dados))
            .build();
    }

    private static String campoDoInstante(TipoEvento tipo) {
        return switch (tipo) {
            case EXECUCAO_ENFILEIRADA -> "enfileiradaEm";
            case DIAGNOSTICO_INICIADO, REPARO_INICIADO -> "iniciadoEm";
            case DIAGNOSTICO_CONCLUIDO -> "concluidoEm";
            case EXECUCAO_FINALIZADA -> "finalizadaEm";
            case EXECUCAO_FALHOU -> "ocorridaEm";
            case EXECUCAO_CANCELADA -> "canceladaEm";
            case ORCAMENTO_APROVADO, PAGAMENTO_APROVADO -> "aprovadoEm";
            case ORCAMENTO_REPROVADO -> "reprovadoEm";
            case ORCAMENTO_EXPIRADO -> "expiradoEm";
            case PAGAMENTO_ESTORNADO -> "estornadoEm";
            case ORCAMENTO_GERADO, ORCAMENTO_CANCELADO, PAGAMENTO_RECUSADO, COMANDO_REJEITADO -> null;
        };
    }

    private static String detalhe(TipoEvento tipo, JsonNode dados) {
        var texto = switch (tipo) {
            case COMANDO_REJEITADO -> juntar(texto(dados, "motivo"), texto(dados, "codigo"));
            case DIAGNOSTICO_CONCLUIDO -> texto(dados, "observacoes");
            case EXECUCAO_ENFILEIRADA -> texto(dados, "etapa");
            default -> texto(dados, "motivo");
        };
        if (texto == null) {
            return null;
        }
        return texto.length() <= TAMANHO_MAXIMO_DETALHE ? texto : texto.substring(0, TAMANHO_MAXIMO_DETALHE);
    }

    private static String juntar(String motivo, String codigo) {
        if (codigo == null) {
            return motivo;
        }
        return motivo == null ? codigo : "%s (%s)".formatted(motivo, codigo);
    }

    private static EtapaExecucao etapa(JsonNode dados) {
        var etapa = texto(dados, "etapa");
        if (etapa == null) {
            return null;
        }
        try {
            return EtapaExecucao.valueOf(etapa);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant instante(JsonNode dados, String campo) {
        var valor = campo == null ? null : texto(dados, campo);
        if (valor == null) {
            return null;
        }
        try {
            return Instant.parse(valor);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String texto(JsonNode dados, String campo) {
        if (dados == null) {
            return null;
        }
        var no = dados.get(campo);
        return no == null || no.isNull() ? null : no.asText();
    }
}
