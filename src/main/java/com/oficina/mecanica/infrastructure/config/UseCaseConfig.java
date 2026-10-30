package com.oficina.mecanica.infrastructure.config;

import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.application.usecase.cliente.ClienteUseCase;
import com.oficina.mecanica.application.usecase.ordemservico.*;
import com.oficina.mecanica.application.usecase.peca.PecaUseCase;
import com.oficina.mecanica.application.usecase.relatorio.RelatorioTempoMedioUseCase;
import com.oficina.mecanica.application.usecase.servico.ServicoUseCase;
import com.oficina.mecanica.application.usecase.veiculo.VeiculoUseCase;
import com.oficina.mecanica.domain.repository.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ClienteUseCase clienteUseCase(ClienteRepository r, UnidadeDeTrabalho u) { return new ClienteUseCase(r, u); }

    @Bean
    public VeiculoUseCase veiculoUseCase(VeiculoRepository vr, ClienteRepository cr, UnidadeDeTrabalho u) {
        return new VeiculoUseCase(vr, cr, u);
    }

    @Bean
    public ServicoUseCase servicoUseCase(ServicoRepository r, UnidadeDeTrabalho u) { return new ServicoUseCase(r, u); }

    @Bean
    public PecaUseCase pecaUseCase(PecaRepository r, UnidadeDeTrabalho u) { return new PecaUseCase(r, u); }

    @Bean
    public CriarOrdemServicoUseCase criarOrdemServicoUseCase(OrdemServicoRepository os,
                                                              ClienteRepository c,
                                                              VeiculoRepository v,
                                                              UnidadeDeTrabalho u) {
        return new CriarOrdemServicoUseCase(os, c, v, u);
    }

    @Bean
    public AdicionarServicoAOSUseCase adicionarServicoAOSUseCase(OrdemServicoRepository os,
                                                                   ServicoRepository s,
                                                                   UnidadeDeTrabalho u) {
        return new AdicionarServicoAOSUseCase(os, s, u);
    }

    @Bean
    public AdicionarPecaAOSUseCase adicionarPecaAOSUseCase(OrdemServicoRepository os,
                                                             PecaRepository p,
                                                             UnidadeDeTrabalho u) {
        return new AdicionarPecaAOSUseCase(os, p, u);
    }

    @Bean
    public CompensacaoEstornoEstoque compensacaoEstornoEstoque(PecaRepository p) {
        return new CompensacaoEstornoEstoque(p);
    }

    @Bean
    public EntregarVeiculoUseCase entregarVeiculoUseCase(OrdemServicoRepository r, UnidadeDeTrabalho u) {
        return new EntregarVeiculoUseCase(r, u);
    }

    @Bean
    public ConsultarStatusOSUseCase consultarStatusOSUseCase(OrdemServicoRepository r) {
        return new ConsultarStatusOSUseCase(r);
    }

    @Bean
    public RelatorioTempoMedioUseCase relatorioTempoMedioUseCase(OrdemServicoRepository r) {
        return new RelatorioTempoMedioUseCase(r);
    }
}
