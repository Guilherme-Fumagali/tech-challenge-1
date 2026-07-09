package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.ordemservico.AprovarOrcamentoExternoUseCase;
import com.oficina.mecanica.application.usecase.ordemservico.ReprovarOrcamentoExternoUseCase;
import com.oficina.mecanica.infrastructure.web.dto.request.AprovarOrcamentoExternoRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.OrdemServicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ordens")
@Tag(name = "Ordens de Serviço — Aprovação Externa")
public class OrdemServicoAprovacaoExternaController {

    private final AprovarOrcamentoExternoUseCase aprovarExterno;
    private final ReprovarOrcamentoExternoUseCase reprovarExterno;

    public OrdemServicoAprovacaoExternaController(AprovarOrcamentoExternoUseCase aprovarExterno,
                                                   ReprovarOrcamentoExternoUseCase reprovarExterno) {
        this.aprovarExterno = aprovarExterno;
        this.reprovarExterno = reprovarExterno;
    }

    @PostMapping("/{id}/aprovar-externo")
    @Operation(summary = "Aprovar ou reprovar orçamento via token enviado por e-mail (endpoint público — sem autenticação)")
    public ResponseEntity<OrdemServicoResponse> aprovarExterno(@PathVariable UUID id,
                                                                @Valid @RequestBody AprovarOrcamentoExternoRequest req) {
        var os = switch (req.decisao()) {
            case APROVAR -> aprovarExterno.executar(id, req.token());
            case REPROVAR -> reprovarExterno.executar(id, req.token());
        };
        return ResponseEntity.ok(OrdemServicoResponse.from(os));
    }
}
