package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.ordemservico.EntregarVeiculoUseCase;
import com.oficina.mecanica.application.usecase.relatorio.RelatorioTempoMedioUseCase;
import com.oficina.mecanica.infrastructure.web.dto.response.OrdemServicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordens")
@Tag(name = "Ordens de Serviço — Ciclo de Vida")
@SecurityRequirement(name = "bearerAuth")
public class OrdemServicoCicloVidaController {

    private final EntregarVeiculoUseCase entregar;
    private final RelatorioTempoMedioUseCase relatorio;

    public OrdemServicoCicloVidaController(EntregarVeiculoUseCase entregar,
                                            RelatorioTempoMedioUseCase relatorio) {
        this.entregar = entregar;
        this.relatorio = relatorio;
    }

    @PostMapping("/{id}/entregar")
    @Operation(summary = "Registrar entrega do veículo ao cliente")
    public ResponseEntity<OrdemServicoResponse> entregar(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(entregar.executar(id)));
    }

    @GetMapping("/relatorio/tempo-medio")
    @Operation(summary = "Relatório de tempo médio de execução por período")
    public ResponseEntity<RelatorioTempoMedioUseCase.ResultadoTempoMedio> relatorio(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return ResponseEntity.ok(relatorio.executar(inicio, fim));
    }
}
