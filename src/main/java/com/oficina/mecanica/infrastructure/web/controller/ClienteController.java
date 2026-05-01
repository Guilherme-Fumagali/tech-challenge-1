package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.cliente.ClienteUseCase;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.infrastructure.web.dto.request.AtualizarClienteRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarClienteRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.ClienteResponse;
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
@RequestMapping("/api/clientes")
@Tag(name = "Clientes")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final ClienteUseCase useCase;

    public ClienteController(ClienteUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar cliente")
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody CriarClienteRequest req) {
        var cliente = useCase.cadastrar(new CpfCnpj(req.cpfCnpj()), req.nome(), req.email(), req.telefone());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponse.from(cliente));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public ResponseEntity<ClienteResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ClienteResponse.from(useCase.buscarPorId(id)));
    }

    @GetMapping
    @Operation(summary = "Listar todos os clientes")
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(useCase.listarTodos().stream().map(ClienteResponse::from).toList());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do cliente")
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable UUID id,
                                                     @Valid @RequestBody AtualizarClienteRequest req) {
        var cliente = useCase.atualizar(id, req.nome(), req.email(), req.telefone());
        return ResponseEntity.ok(ClienteResponse.from(cliente));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover cliente")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        useCase.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
