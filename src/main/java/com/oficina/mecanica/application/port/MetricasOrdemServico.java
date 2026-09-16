package com.oficina.mecanica.application.port;

import com.oficina.mecanica.domain.entity.TransicaoOS;

import java.util.UUID;

public interface MetricasOrdemServico {

    void registrarAbertura(UUID ordemServicoId);

    void registrarTransicao(TransicaoOS transicao);

    void registrarFalhaTransicao(String motivo);

    void registrarFalhaIntegracao(String integracao, String motivo);
}
