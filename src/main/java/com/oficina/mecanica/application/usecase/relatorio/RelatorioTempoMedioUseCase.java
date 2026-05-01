package com.oficina.mecanica.application.usecase.relatorio;

import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;

public class RelatorioTempoMedioUseCase {

    private final OrdemServicoRepository repository;

    public RelatorioTempoMedioUseCase(OrdemServicoRepository repository) {
        this.repository = repository;
    }

    public ResultadoTempoMedio executar(LocalDateTime inicio, LocalDateTime fim) {
        var ordens = repository.listarFinalizadasNoPeriodo(inicio, fim);
        OptionalDouble mediaHoras = ordens.stream()
            .filter(os -> os.getDataInicio() != null && os.getDataConclusao() != null)
            .mapToLong(os -> Duration.between(os.getDataInicio(), os.getDataConclusao()).toMinutes())
            .average();

        return new ResultadoTempoMedio(
            ordens.size(),
            mediaHoras.isPresent() ? mediaHoras.getAsDouble() / 60.0 : 0.0,
            inicio,
            fim
        );
    }

    public record ResultadoTempoMedio(
        int totalOrdens,
        double tempoMedioHoras,
        LocalDateTime periodoInicio,
        LocalDateTime periodoFim
    ) {}
}
