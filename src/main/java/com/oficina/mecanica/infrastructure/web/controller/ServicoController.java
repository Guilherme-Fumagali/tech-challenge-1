package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.servico.ServicoUseCase;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarServicoRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.ServicoResponse;
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
@RequestMapping("/api/servicos")
@Tag(name = "Serviços")
@SecurityRequirement(name = "bearerAuth")
public class ServicoController {

    private final ServicoUseCase useCase;

    public ServicoController(ServicoUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar serviço no catálogo")
    public ResponseEntity<ServicoResponse> cadastrar(@Valid @RequestBody CriarServicoRequest req) {
        var servico = useCase.cadastrar(req.nome(), req.descricao(), req.precoUnitario(), req.tempoEstimadoHoras());
        return ResponseEntity.status(HttpStatus.CREATED).body(ServicoResponse.from(servico));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço por ID")
    public ResponseEntity<ServicoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ServicoResponse.from(useCase.buscarPorId(id)));
    }

    @GetMapping
    @Operation(summary = "Listar catálogo de serviços")
    public ResponseEntity<List<ServicoResponse>> listar() {
        return ResponseEntity.ok(useCase.listarTodos().stream().map(ServicoResponse::from).toList());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço")
    public ResponseEntity<ServicoResponse> atualizar(@PathVariable UUID id,
                                                     @Valid @RequestBody CriarServicoRequest req) {
        var servico = useCase.atualizar(id, req.nome(), req.descricao(), req.precoUnitario(), req.tempoEstimadoHoras());
        return ResponseEntity.ok(ServicoResponse.from(servico));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover serviço")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        useCase.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
