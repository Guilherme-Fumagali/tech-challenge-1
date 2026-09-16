package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.veiculo.VeiculoUseCase;
import com.oficina.mecanica.domain.valueobject.Placa;
import com.oficina.mecanica.infrastructure.security.Solicitante;
import com.oficina.mecanica.infrastructure.web.dto.request.AtualizarVeiculoRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarVeiculoRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.VeiculoResponse;
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
@RequestMapping("/api/veiculos")
@Tag(name = "Veículos")
@SecurityRequirement(name = "bearerAuth")
public class VeiculoController {

    private final VeiculoUseCase useCase;

    public VeiculoController(VeiculoUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar veículo")
    public ResponseEntity<VeiculoResponse> cadastrar(@Valid @RequestBody CriarVeiculoRequest req) {
        var veiculo = useCase.cadastrar(new Placa(req.placa()), req.marca(), req.modelo(),
            req.anoFabricacao(), req.clienteId());
        return ResponseEntity.status(HttpStatus.CREATED).body(VeiculoResponse.from(veiculo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID (cliente: apenas os próprios)")
    public ResponseEntity<VeiculoResponse> buscar(@PathVariable UUID id) {
        var solicitante = Solicitante.atual();
        var veiculo = solicitante.funcionario()
            ? useCase.buscarPorId(id)
            : useCase.buscarPorIdDoCliente(id, solicitante.id());
        return ResponseEntity.ok(VeiculoResponse.from(veiculo));
    }

    @GetMapping
    @Operation(summary = "Listar veículos (funcionário: todos; cliente: os próprios)")
    public ResponseEntity<List<VeiculoResponse>> listar() {
        var solicitante = Solicitante.atual();
        var veiculos = solicitante.funcionario()
            ? useCase.listarTodos()
            : useCase.listarPorCliente(solicitante.id());
        return ResponseEntity.ok(veiculos.stream().map(VeiculoResponse::from).toList());
    }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Listar veículos de um cliente")
    public ResponseEntity<List<VeiculoResponse>> listarPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(useCase.listarPorCliente(clienteId).stream()
            .map(VeiculoResponse::from).toList());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar veículo")
    public ResponseEntity<VeiculoResponse> atualizar(@PathVariable UUID id,
                                                     @Valid @RequestBody AtualizarVeiculoRequest req) {
        var veiculo = useCase.atualizar(id, req.marca(), req.modelo(), req.anoFabricacao());
        return ResponseEntity.ok(VeiculoResponse.from(veiculo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover veículo")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        useCase.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
