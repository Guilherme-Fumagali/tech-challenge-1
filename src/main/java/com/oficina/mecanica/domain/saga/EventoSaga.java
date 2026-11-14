package com.oficina.mecanica.domain.saga;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record EventoSaga(
    UUID mensagemId,
    TipoEvento tipo,
    Instant ocorridoEm,
    Instant instanteDoFato,
    EtapaExecucao etapa,
    String comandoRejeitado,
    String detalhe
) {

    public Instant instanteDoFatoOuOcorrencia() {
        return instanteDoFato != null ? instanteDoFato : ocorridoEm;
    }
}
