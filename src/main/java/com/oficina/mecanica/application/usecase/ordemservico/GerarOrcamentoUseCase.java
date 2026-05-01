package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.application.port.NotificacaoService;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;

import java.util.UUID;

public class GerarOrcamentoUseCase {

    private final OrdemServicoRepository repository;
    private final NotificacaoService notificacaoService;

    public GerarOrcamentoUseCase(OrdemServicoRepository repository,
                                 NotificacaoService notificacaoService) {
        this.repository = repository;
        this.notificacaoService = notificacaoService;
    }

    public OrdemServico executar(UUID osId) {
        var os = repository.buscarPorId(osId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", osId));

        os.gerarOrcamento();
        var osSalva = repository.salvar(os);

        // Dispara notificação após persistir — canal concreto é definido pelo adaptador injetado
        notificacaoService.notificarOrcamentoPendente(
            osSalva.getId(),
            osSalva.getClienteId(),
            osSalva.calcularOrcamento()
        );

        return osSalva;
    }
}
