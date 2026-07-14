package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.application.usecase.ordemservico.AprovarOrcamentoExternoUseCase;
import com.oficina.mecanica.application.usecase.ordemservico.ReprovarOrcamentoExternoUseCase;
import com.oficina.mecanica.infrastructure.web.dto.request.AprovarOrcamentoExternoRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.OrdemServicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Operation(summary = "Aprovar ou reprovar orçamento via token (API — endpoint público, sem autenticação)")
    public ResponseEntity<OrdemServicoResponse> aprovarExterno(@PathVariable UUID id,
                                                                @Valid @RequestBody AprovarOrcamentoExternoRequest req) {
        var os = switch (req.decisao()) {
            case APROVAR -> aprovarExterno.executar(id, req.token());
            case REPROVAR -> reprovarExterno.executar(id, req.token());
        };
        return ResponseEntity.ok(OrdemServicoResponse.from(os));
    }

    // Endpoints GET abaixo existem para os BOTÕES do e-mail (um link só pode ser GET).
    // Executam a decisão e devolvem uma página HTML amigável para o cliente.

    @GetMapping(value = "/{id}/aprovar-externo", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Aprovar orçamento pelo link do e-mail (público)")
    public ResponseEntity<String> aprovarPeloLink(@PathVariable UUID id, @RequestParam String token) {
        try {
            aprovarExterno.executar(id, token);
            return html(paginaResultado(true, "Orçamento aprovado!",
                "Recebemos sua aprovação. Já vamos iniciar o serviço no seu veículo. Obrigado!"));
        } catch (RuntimeException e) {
            return html(paginaErro(e.getMessage()));
        }
    }

    @GetMapping(value = "/{id}/reprovar-externo", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Reprovar orçamento pelo link do e-mail (público)")
    public ResponseEntity<String> reprovarPeloLink(@PathVariable UUID id, @RequestParam String token) {
        try {
            reprovarExterno.executar(id, token);
            return html(paginaResultado(false, "Orçamento reprovado",
                "Registramos que você não aprovou o orçamento. Nenhum serviço será executado. Qualquer dúvida, fale com a oficina."));
        } catch (RuntimeException e) {
            return html(paginaErro(e.getMessage()));
        }
    }

    private ResponseEntity<String> html(String corpo) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(corpo);
    }

    private String paginaResultado(boolean aprovado, String titulo, String texto) {
        var cor = aprovado ? "#16a34a" : "#dc2626";
        var icone = aprovado ? "&#10003;" : "&#10005;";
        return pagina(cor, icone, titulo, texto);
    }

    private String paginaErro(String detalhe) {
        return pagina("#d97706", "!",
            "Não foi possível processar",
            (detalhe == null ? "Link inválido ou expirado." : detalhe)
                + " Se o problema persistir, entre em contato com a oficina.");
    }

    private String pagina(String cor, String icone, String titulo, String texto) {
        return """
            <!doctype html>
            <html lang="pt-BR"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>%s</title></head>
            <body style="margin:0;background:#f1f5f9;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
              <div style="max-width:480px;margin:48px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,.08);">
                <div style="background:%s;height:8px;"></div>
                <div style="padding:40px 32px;text-align:center;">
                  <div style="width:72px;height:72px;line-height:72px;border-radius:50%%;background:%s;color:#fff;font-size:36px;margin:0 auto 20px;">%s</div>
                  <h1 style="margin:0 0 12px;font-size:22px;color:#0f172a;">%s</h1>
                  <p style="margin:0;font-size:15px;line-height:1.6;color:#475569;">%s</p>
                </div>
                <div style="padding:16px;text-align:center;background:#f8fafc;font-size:12px;color:#94a3b8;">
                  Oficina Mecânica &middot; Tech Challenge
                </div>
              </div>
            </body></html>
            """.formatted(titulo, cor, cor, icone, titulo, texto);
    }
}
