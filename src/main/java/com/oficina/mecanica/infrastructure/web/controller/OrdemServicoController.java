package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.ordemservico.*;
import com.oficina.mecanica.infrastructure.web.dto.request.AdicionarItemRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarOrdemServicoRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.OrdemServicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordens")
@Tag(name = "Ordens de Serviço")
@SecurityRequirement(name = "bearerAuth")
public class OrdemServicoController {

    private final CriarOrdemServicoUseCase criarOS;
    private final ConsultarStatusOSUseCase consultar;
    private final AdicionarServicoAOSUseCase adicionarServico;
    private final AdicionarPecaAOSUseCase adicionarPeca;

    public OrdemServicoController(CriarOrdemServicoUseCase criarOS,
                                  ConsultarStatusOSUseCase consultar,
                                  AdicionarServicoAOSUseCase adicionarServico,
                                  AdicionarPecaAOSUseCase adicionarPeca) {
        this.criarOS = criarOS;
        this.consultar = consultar;
        this.adicionarServico = adicionarServico;
        this.adicionarPeca = adicionarPeca;
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
}
