package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.identidade.ConsultarIdentidadeUseCase;
import com.oficina.mecanica.infrastructure.web.dto.response.IdentidadeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/identidades")
@Tag(name = "Identidades (uso interno)")
@SecurityRequirement(name = "chaveInterna")
public class IdentidadeInternaController {

    private final ConsultarIdentidadeUseCase useCase;

    public IdentidadeInternaController(ConsultarIdentidadeUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/clientes/{cpfCnpj}")
    @Operation(summary = "Identidade do cliente por CPF ou CNPJ, consultada pela função de autenticação")
    public ResponseEntity<IdentidadeResponse> cliente(@PathVariable String cpfCnpj) {
        return ResponseEntity.ok(IdentidadeResponse.from(useCase.cliente(cpfCnpj)));
    }

    @GetMapping("/funcionarios/{cpf}")
    @Operation(summary = "Identidade do funcionário por CPF, consultada pela função de autenticação")
    public ResponseEntity<IdentidadeResponse> funcionario(@PathVariable String cpf) {
        return ResponseEntity.ok(IdentidadeResponse.from(useCase.funcionario(cpf)));
    }
}
