package com.oficina.mecanica.application.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface NotificacaoService {

    void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal);
}
