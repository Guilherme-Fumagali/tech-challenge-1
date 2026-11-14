package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.application.port.ConsultaExecucao;
import com.oficina.mecanica.application.port.ConsultaOrcamento;
import com.oficina.mecanica.domain.repository.HistoricoStatusRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public class ConsultarAcompanhamentoUseCase {

    public static final String FONTE_BILLING = "billing";
    public static final String FONTE_EXECUTION = "execution";

    private final ConsultarStatusOSUseCase consultarOrdem;
    private final HistoricoStatusRepository historicoRepository;
    private final ConsultaOrcamento consultaOrcamento;
    private final ConsultaExecucao consultaExecucao;

    public ConsultarAcompanhamentoUseCase(ConsultarStatusOSUseCase consultarOrdem,
                                          HistoricoStatusRepository historicoRepository,
                                          ConsultaOrcamento consultaOrcamento,
                                          ConsultaExecucao consultaExecucao) {
        this.consultarOrdem = consultarOrdem;
        this.historicoRepository = historicoRepository;
        this.consultaOrcamento = consultaOrcamento;
        this.consultaExecucao = consultaExecucao;
    }

    public Acompanhamento executar(UUID osId, UUID clienteId, String autorizacao) {
        var os = clienteId == null ? consultarOrdem.executar(osId) : consultarOrdem.executarDoCliente(osId, clienteId);
        var orcamento = iniciar(() -> consultaOrcamento.buscarPorOrdemServico(osId, autorizacao));
        var execucao = iniciar(() -> consultaExecucao.buscarPorOrdemServico(osId, autorizacao));
        var historico = historicoRepository.listarPorOrdemServico(osId);

        var indisponiveis = new ArrayList<String>();
        var dadosOrcamento = aguardar(orcamento, FONTE_BILLING, indisponiveis);
        var dadosExecucao = aguardar(execucao, FONTE_EXECUTION, indisponiveis);
        return new Acompanhamento(os, historico, dadosOrcamento, dadosExecucao, List.copyOf(indisponiveis));
    }

    private static CompletableFuture<Optional<Map<String, Object>>> iniciar(
            Supplier<CompletableFuture<Optional<Map<String, Object>>>> consulta) {
        try {
            return consulta.get();
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static Map<String, Object> aguardar(CompletableFuture<Optional<Map<String, Object>>> consulta, String fonte,
                                                List<String> indisponiveis) {
        try {
            return consulta.join().orElse(null);
        } catch (CompletionException | CancellationException e) {
            indisponiveis.add(fonte);
            return null;
        }
    }
}
