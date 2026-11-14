package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.ordemservico.*;
import com.oficina.mecanica.application.usecase.saga.ConsultarSagaUseCase;
import com.oficina.mecanica.infrastructure.security.Solicitante;
import com.oficina.mecanica.infrastructure.web.dto.request.AdicionarItemRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarOrdemServicoRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.AcompanhamentoResponse;
import com.oficina.mecanica.infrastructure.web.dto.response.HistoricoStatusResponse;
import com.oficina.mecanica.infrastructure.web.dto.response.OrdemServicoAbertaResponse;
import com.oficina.mecanica.infrastructure.web.dto.response.OrdemServicoResponse;
import com.oficina.mecanica.infrastructure.web.dto.response.SagaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
    private final ConsultarHistoricoOSUseCase historico;
    private final ConsultarSagaUseCase saga;
    private final ConsultarAcompanhamentoUseCase acompanhamento;

    public OrdemServicoController(CriarOrdemServicoUseCase criarOS,
                                  ConsultarStatusOSUseCase consultar,
                                  AdicionarServicoAOSUseCase adicionarServico,
                                  AdicionarPecaAOSUseCase adicionarPeca,
                                  ConsultarHistoricoOSUseCase historico,
                                  ConsultarSagaUseCase saga,
                                  ConsultarAcompanhamentoUseCase acompanhamento) {
        this.criarOS = criarOS;
        this.consultar = consultar;
        this.adicionarServico = adicionarServico;
        this.adicionarPeca = adicionarPeca;
        this.historico = historico;
        this.saga = saga;
        this.acompanhamento = acompanhamento;
    }

    @PostMapping
    @Operation(summary = "Abrir nova Ordem de Serviço e iniciar a saga (pedido de diagnóstico ao Execution)")
    public ResponseEntity<OrdemServicoAbertaResponse> criar(@Valid @RequestBody CriarOrdemServicoRequest req) {
        var abertura = criarOS.executar(req.clienteId(), req.veiculoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrdemServicoAbertaResponse.from(abertura));
    }

    @GetMapping
    @Operation(summary = "Listar Ordens de Serviço (funcionário: ativas; cliente: as próprias)")
    public ResponseEntity<List<OrdemServicoResponse>> listar() {
        var solicitante = Solicitante.atual();
        var ordens = solicitante.funcionario()
            ? consultar.listarTodas()
            : consultar.listarDoCliente(solicitante.id());
        return ResponseEntity.ok(ordens.stream().map(OrdemServicoResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes de uma OS (cliente: apenas as próprias)")
    public ResponseEntity<OrdemServicoResponse> buscar(@PathVariable UUID id) {
        var solicitante = Solicitante.atual();
        var os = solicitante.funcionario()
            ? consultar.executar(id)
            : consultar.executarDoCliente(id, solicitante.id());
        return ResponseEntity.ok(OrdemServicoResponse.from(os));
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico de status da OS, com o evento que causou cada transição (cliente: apenas as próprias)")
    public ResponseEntity<List<HistoricoStatusResponse>> historico(@PathVariable UUID id) {
        var solicitante = Solicitante.atual();
        var registros = solicitante.funcionario()
            ? historico.executar(id)
            : historico.executarDoCliente(id, solicitante.id());
        return ResponseEntity.ok(registros.stream().map(HistoricoStatusResponse::from).toList());
    }

    @GetMapping("/{id}/acompanhamento")
    @Operation(summary = "Acompanhamento composto: OS e histórico locais, orçamento e pagamento (Billing) e execução "
        + "(Execution); fonte indisponível vem nula e listada em fontesIndisponiveis (cliente: apenas as próprias)")
    public ResponseEntity<AcompanhamentoResponse> acompanhamento(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String autorizacao) {
        var solicitante = Solicitante.atual();
        var resultado = acompanhamento.executar(id, solicitante.funcionario() ? null : solicitante.id(), autorizacao);
        return ResponseEntity.ok(AcompanhamentoResponse.from(resultado));
    }

    @GetMapping("/{id}/saga")
    @Operation(summary = "Estado da saga da OS e passos trocados com os participantes")
    public ResponseEntity<SagaResponse> saga(@PathVariable UUID id) {
        return ResponseEntity.ok(SagaResponse.from(saga.executar(id)));
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
