package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.ordemservico.*;
import com.oficina.mecanica.application.usecase.relatorio.RelatorioTempoMedioUseCase;
import com.oficina.mecanica.infrastructure.web.dto.request.AdicionarItemRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarOrdemServicoRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.OrdemServicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordens")
@Tag(name = "Ordens de Serviço")
@SecurityRequirement(name = "bearerAuth")
public class OrdemServicoController {

    private final CriarOrdemServicoUseCase criarOS;
    private final IniciarDiagnosticoUseCase iniciarDiagnostico;
    private final AdicionarServicoAOSUseCase adicionarServico;
    private final AdicionarPecaAOSUseCase adicionarPeca;
    private final GerarOrcamentoUseCase gerarOrcamento;
    private final AprovarOrcamentoUseCase aprovar;
    private final ReprovarOrcamentoUseCase reprovar;
    private final ConcluirServicosUseCase concluir;
    private final EntregarVeiculoUseCase entregar;
    private final ConsultarStatusOSUseCase consultar;
    private final RelatorioTempoMedioUseCase relatorio;

    public OrdemServicoController(CriarOrdemServicoUseCase criarOS,
                                  IniciarDiagnosticoUseCase iniciarDiagnostico,
                                  AdicionarServicoAOSUseCase adicionarServico,
                                  AdicionarPecaAOSUseCase adicionarPeca,
                                  GerarOrcamentoUseCase gerarOrcamento,
                                  AprovarOrcamentoUseCase aprovar,
                                  ReprovarOrcamentoUseCase reprovar,
                                  ConcluirServicosUseCase concluir,
                                  EntregarVeiculoUseCase entregar,
                                  ConsultarStatusOSUseCase consultar,
                                  RelatorioTempoMedioUseCase relatorio) {
        this.criarOS = criarOS;
        this.iniciarDiagnostico = iniciarDiagnostico;
        this.adicionarServico = adicionarServico;
        this.adicionarPeca = adicionarPeca;
        this.gerarOrcamento = gerarOrcamento;
        this.aprovar = aprovar;
        this.reprovar = reprovar;
        this.concluir = concluir;
        this.entregar = entregar;
        this.consultar = consultar;
        this.relatorio = relatorio;
    }

    @PostMapping
    @Operation(summary = "Abrir nova Ordem de Serviço")
    public ResponseEntity<OrdemServicoResponse> criar(@Valid @RequestBody CriarOrdemServicoRequest req) {
        var os = criarOS.executar(req.clienteId(), req.veiculoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrdemServicoResponse.from(os));
    }

    @GetMapping
    @Operation(summary = "Listar todas as Ordens de Serviço")
    public ResponseEntity<List<OrdemServicoResponse>> listar() {
        return ResponseEntity.ok(consultar.listarTodas().stream()
            .map(OrdemServicoResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes de uma OS")
    public ResponseEntity<OrdemServicoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(consultar.executar(id)));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Consultar status de uma OS (endpoint público — sem autenticação)")
    public ResponseEntity<OrdemServicoResponse> consultarStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(consultar.executar(id)));
    }

    @PostMapping("/{id}/iniciar-diagnostico")
    @Operation(summary = "Iniciar diagnóstico da OS")
    public ResponseEntity<OrdemServicoResponse> iniciarDiagnostico(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(iniciarDiagnostico.executar(id)));
    }

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Adicionar serviço à OS (snapshot de preço aplicado)")
    public ResponseEntity<OrdemServicoResponse> adicionarServico(@PathVariable UUID id,
                                                                  @Valid @RequestBody AdicionarItemRequest req) {
        return ResponseEntity.ok(OrdemServicoResponse.from(
            adicionarServico.executar(id, req.itemId(), req.quantidade())));
    }

    @PostMapping("/{id}/pecas")
    @Operation(summary = "Adicionar peça à OS (decrementa estoque atomicamente)")
    public ResponseEntity<OrdemServicoResponse> adicionarPeca(@PathVariable UUID id,
                                                               @Valid @RequestBody AdicionarItemRequest req) {
        return ResponseEntity.ok(OrdemServicoResponse.from(
            adicionarPeca.executar(id, req.itemId(), req.quantidade())));
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
