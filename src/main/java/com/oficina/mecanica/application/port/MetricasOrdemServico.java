package com.oficina.mecanica.application.port;

import com.oficina.mecanica.domain.entity.TransicaoOS;

public interface MetricasOrdemServico {

    void registrarAbertura();

    void registrarTransicao(TransicaoOS transicao);

    void registrarFalhaTransicao(String motivo);

    void registrarFalhaIntegracao(String integracao, String motivo);
}
