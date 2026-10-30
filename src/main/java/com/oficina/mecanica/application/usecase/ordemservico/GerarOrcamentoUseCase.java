package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.application.port.NotificacaoService;
import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;

import java.time.Duration;
import java.util.UUID;

public class GerarOrcamentoUseCase {

    private final OrdemServicoRepository repository;
    private final NotificacaoService notificacaoService;
    private final Duration validadeTokenAprovacao;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public GerarOrcamentoUseCase(OrdemServicoRepository repository,
                                 NotificacaoService notificacaoService,
                                 Duration validadeTokenAprovacao,
                                 UnidadeDeTrabalho unidadeDeTrabalho) {
        this.repository = repository;
        this.notificacaoService = notificacaoService;
        this.validadeTokenAprovacao = validadeTokenAprovacao;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
    }

    public OrdemServico executar(UUID osId) {
        var osSalva = unidadeDeTrabalho.executar(() -> {
            var os = repository.buscarPorId(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));
            os.gerarOrcamento(validadeTokenAprovacao);
            return repository.salvar(os);
        });

        notificacaoService.notificarOrcamentoPendente(
            osSalva.getId(),
            osSalva.getClienteId(),
            osSalva.calcularOrcamento(),
            osSalva.getTokenAprovacaoExterna()
        );

        return osSalva;
    }
}
