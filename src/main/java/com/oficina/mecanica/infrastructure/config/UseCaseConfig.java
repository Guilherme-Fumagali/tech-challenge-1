package com.oficina.mecanica.infrastructure.config;

import com.oficina.mecanica.application.port.NotificacaoService;
import com.oficina.mecanica.application.usecase.cliente.ClienteUseCase;
import com.oficina.mecanica.application.usecase.ordemservico.*;
import com.oficina.mecanica.application.usecase.peca.PecaUseCase;
import com.oficina.mecanica.application.usecase.relatorio.RelatorioTempoMedioUseCase;
import com.oficina.mecanica.application.usecase.servico.ServicoUseCase;
import com.oficina.mecanica.application.usecase.veiculo.VeiculoUseCase;
import com.oficina.mecanica.domain.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ClienteUseCase clienteUseCase(ClienteRepository r) { return new ClienteUseCase(r); }

    @Bean
    public VeiculoUseCase veiculoUseCase(VeiculoRepository vr, ClienteRepository cr) {
        return new VeiculoUseCase(vr, cr);
    }

    @Bean
    public ServicoUseCase servicoUseCase(ServicoRepository r) { return new ServicoUseCase(r); }

    @Bean
    public PecaUseCase pecaUseCase(PecaRepository r) { return new PecaUseCase(r); }

    @Bean
    public CriarOrdemServicoUseCase criarOrdemServicoUseCase(OrdemServicoRepository os,
                                                              ClienteRepository c,
                                                              VeiculoRepository v) {
        return new CriarOrdemServicoUseCase(os, c, v);
    }

    @Bean
    public IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase(OrdemServicoRepository r) {
        return new IniciarDiagnosticoUseCase(r);
    }

    @Bean
    public AdicionarServicoAOSUseCase adicionarServicoAOSUseCase(OrdemServicoRepository os,
                                                                   ServicoRepository s) {
        return new AdicionarServicoAOSUseCase(os, s);
    }

    @Bean
    public AdicionarPecaAOSUseCase adicionarPecaAOSUseCase(OrdemServicoRepository os,
                                                             PecaRepository p) {
        return new AdicionarPecaAOSUseCase(os, p);
    }

    @Bean
    public GerarOrcamentoUseCase gerarOrcamentoUseCase(OrdemServicoRepository r,
                                                        NotificacaoService n,
                                                        @Value("${app.aprovacao-externa.validade-horas}") long validadeHoras) {
        return new GerarOrcamentoUseCase(r, n, Duration.ofHours(validadeHoras));
    }

    @Bean
    public AprovarOrcamentoUseCase aprovarOrcamentoUseCase(OrdemServicoRepository r) {
        return new AprovarOrcamentoUseCase(r);
    }

    @Bean
    public ReprovarOrcamentoUseCase reprovarOrcamentoUseCase(OrdemServicoRepository os,
                                                               PecaRepository p) {
        return new ReprovarOrcamentoUseCase(os, p);
    }

    @Bean
    public AprovarOrcamentoExternoUseCase aprovarOrcamentoExternoUseCase(OrdemServicoRepository r) {
        return new AprovarOrcamentoExternoUseCase(r);
    }

    @Bean
    public ReprovarOrcamentoExternoUseCase reprovarOrcamentoExternoUseCase(OrdemServicoRepository os,
                                                                            PecaRepository p) {
        return new ReprovarOrcamentoExternoUseCase(os, p);
    }

    @Bean
    public ConcluirServicosUseCase concluirServicosUseCase(OrdemServicoRepository r) {
        return new ConcluirServicosUseCase(r);
    }

    @Bean
    public EntregarVeiculoUseCase entregarVeiculoUseCase(OrdemServicoRepository r) {
        return new EntregarVeiculoUseCase(r);
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
