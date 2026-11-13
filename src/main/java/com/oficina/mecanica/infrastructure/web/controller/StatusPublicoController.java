package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.ordemservico.ConsultarStatusOSUseCase;
import com.oficina.mecanica.infrastructure.web.dto.response.StatusPublicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ordens")
@Tag(name = "Ordens de Serviço")
public class StatusPublicoController {

    private final ConsultarStatusOSUseCase consultar;

    public StatusPublicoController(ConsultarStatusOSUseCase consultar) {
        this.consultar = consultar;
    }

    @GetMapping("/{id}/status")
    @SecurityRequirements
    @Operation(summary = "Consultar o status de uma OS (público, sem autenticação): apenas id, status e data da última mudança")
    public ResponseEntity<StatusPublicoResponse> consultarStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(StatusPublicoResponse.from(consultar.executar(id)));
    }
}
