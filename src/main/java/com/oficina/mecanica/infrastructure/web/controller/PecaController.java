package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.peca.PecaUseCase;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarPecaRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.IncrementarEstoqueRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.PecaResponse;
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
@RequestMapping("/api/pecas")
@Tag(name = "Peças e Estoque")
@SecurityRequirement(name = "bearerAuth")
public class PecaController {

    private final PecaUseCase useCase;

    public PecaController(PecaUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar peça/insumo no catálogo")
    public ResponseEntity<PecaResponse> cadastrar(@Valid @RequestBody CriarPecaRequest req) {
        var peca = useCase.cadastrar(req.nome(), req.descricao(), req.precoUnitario(),
            req.quantidadeEstoque(), req.estoqueMinimo());
        return ResponseEntity.status(HttpStatus.CREATED).body(PecaResponse.from(peca));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar peça por ID")
    public ResponseEntity<PecaResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(PecaResponse.from(useCase.buscarPorId(id)));
    }

    @GetMapping
    @Operation(summary = "Listar catálogo de peças")
    public ResponseEntity<List<PecaResponse>> listar() {
        return ResponseEntity.ok(useCase.listarTodas().stream().map(PecaResponse::from).toList());
    }

    @GetMapping("/alerta-estoque")
    @Operation(summary = "Listar peças abaixo do estoque mínimo")
    public ResponseEntity<List<PecaResponse>> alertaEstoque() {
        return ResponseEntity.ok(useCase.listarAbaixoDoEstoqueMinimo().stream()
            .map(PecaResponse::from).toList());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar peça")
    public ResponseEntity<PecaResponse> atualizar(@PathVariable UUID id,
                                                   @Valid @RequestBody CriarPecaRequest req) {
        var peca = useCase.atualizar(id, req.nome(), req.descricao(), req.precoUnitario(), req.estoqueMinimo());
        return ResponseEntity.ok(PecaResponse.from(peca));
    }

    @PostMapping("/{id}/estoque/incrementar")
    @Operation(summary = "Entrada de estoque — incrementar quantidade")
    public ResponseEntity<PecaResponse> incrementarEstoque(@PathVariable UUID id,
                                                           @Valid @RequestBody IncrementarEstoqueRequest req) {
        return ResponseEntity.ok(PecaResponse.from(useCase.incrementarEstoque(id, req.quantidade())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover peça")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        useCase.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
