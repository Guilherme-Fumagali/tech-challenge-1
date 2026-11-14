package com.oficina.mecanica.application.port;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ConsultaExecucao {

    CompletableFuture<Optional<Map<String, Object>>> buscarPorOrdemServico(UUID osId, String autorizacao);
}
