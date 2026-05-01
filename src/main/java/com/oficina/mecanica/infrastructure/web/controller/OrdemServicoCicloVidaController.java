package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.ordemservico.*;
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

    private final IniciarDiagnosticoUseCase iniciarDiagnostico;
    private final GerarOrcamentoUseCase gerarOrcamento;
    private final AprovarOrcamentoUseCase aprovar;
    private final ReprovarOrcamentoUseCase reprovar;
    private final ConcluirServicosUseCase concluir;
    private final EntregarVeiculoUseCase entregar;
    private final RelatorioTempoMedioUseCase relatorio;

    public OrdemServicoCicloVidaController(IniciarDiagnosticoUseCase iniciarDiagnostico,
                                            GerarOrcamentoUseCase gerarOrcamento,
                                            AprovarOrcamentoUseCase aprovar,
                                            ReprovarOrcamentoUseCase reprovar,
                                            ConcluirServicosUseCase concluir,
                                            EntregarVeiculoUseCase entregar,
                                            RelatorioTempoMedioUseCase relatorio) {
        this.iniciarDiagnostico = iniciarDiagnostico;
        this.gerarOrcamento = gerarOrcamento;
        this.aprovar = aprovar;
        this.reprovar = reprovar;
        this.concluir = concluir;
        this.entregar = entregar;
        this.relatorio = relatorio;
    }

    @PostMapping("/{id}/iniciar-diagnostico")
    @Operation(summary = "Iniciar diagnóstico da OS")
    public ResponseEntity<OrdemServicoResponse> iniciarDiagnostico(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(iniciarDiagnostico.executar(id)));
    }

    @PostMapping("/{id}/gerar-orcamento")
    @Operation(summary = "Gerar orçamento e enviar para aprovação do cliente")
    public ResponseEntity<OrdemServicoResponse> gerarOrcamento(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(gerarOrcamento.executar(id)));
    }

    @PostMapping("/{id}/aprovar")
    @Operation(summary = "Aprovar orçamento — inicia execução dos serviços")
    public ResponseEntity<OrdemServicoResponse> aprovar(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(aprovar.executar(id)));
    }

    @PostMapping("/{id}/reprovar")
    @Operation(summary = "Reprovar orçamento — cancela a OS e estorna estoque")
    public ResponseEntity<OrdemServicoResponse> reprovar(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(reprovar.executar(id)));
    }

    @PostMapping("/{id}/concluir")
    @Operation(summary = "Concluir serviços — OS finalizada, aguardando entrega")
    public ResponseEntity<OrdemServicoResponse> concluir(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(concluir.executar(id)));
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
