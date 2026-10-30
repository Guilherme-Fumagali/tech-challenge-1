package com.oficina.mecanica.infrastructure.config;

import com.oficina.mecanica.application.port.MetricasSaga;
import com.oficina.mecanica.application.port.PublicadorMensagens;
import com.oficina.mecanica.application.port.RegistroMensagensProcessadas;
import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.application.usecase.cliente.ClienteUseCase;
import com.oficina.mecanica.application.usecase.identidade.ConsultarIdentidadeUseCase;
import com.oficina.mecanica.application.usecase.ordemservico.*;
import com.oficina.mecanica.application.usecase.peca.PecaUseCase;
import com.oficina.mecanica.application.usecase.relatorio.RelatorioTempoMedioUseCase;
import com.oficina.mecanica.application.usecase.saga.ConfiguracaoSaga;
import com.oficina.mecanica.application.usecase.saga.ConsultarSagaUseCase;
import com.oficina.mecanica.application.usecase.saga.MontadorDadosComando;
import com.oficina.mecanica.application.usecase.saga.ProcessarEventoSagaUseCase;
import com.oficina.mecanica.application.usecase.saga.VerificarPrazosSagaUseCase;
import com.oficina.mecanica.application.usecase.servico.ServicoUseCase;
import com.oficina.mecanica.application.usecase.veiculo.VeiculoUseCase;
import com.oficina.mecanica.domain.repository.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(PropriedadesSaga.class)
public class UseCaseConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }

    @Bean
    public ConfiguracaoSaga configuracaoSaga(PropriedadesSaga propriedades) {
        return new ConfiguracaoSaga(propriedades.prazoResposta(), propriedades.maxReenvios(), propriedades.lote());
    }

    @Bean
    public MontadorDadosComando montadorDadosComando(ClienteRepository c, VeiculoRepository v) {
        return new MontadorDadosComando(c, v);
    }

    @Bean
    public ProcessarEventoSagaUseCase processarEventoSagaUseCase(SagaOrdemServicoRepository sagas,
                                                                 OrdemServicoRepository os,
                                                                 CompensacaoEstornoEstoque estorno,
                                                                 MontadorDadosComando montador,
                                                                 PublicadorMensagens publicador,
                                                                 RegistroMensagensProcessadas processadas,
                                                                 MetricasSaga metricas,
                                                                 UnidadeDeTrabalho u,
                                                                 ConfiguracaoSaga configuracao,
                                                                 Clock relogio) {
        return new ProcessarEventoSagaUseCase(sagas, os, estorno, montador, publicador, processadas, metricas, u,
            configuracao, relogio);
    }

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
                                                              SagaOrdemServicoRepository sagas,
                                                              MontadorDadosComando montador,
                                                              PublicadorMensagens publicador,
                                                              MetricasSaga metricas,
                                                              UnidadeDeTrabalho u,
                                                              ConfiguracaoSaga configuracao,
                                                              Clock relogio) {
        return new CriarOrdemServicoUseCase(os, c, v, sagas, montador, publicador, metricas, u, configuracao, relogio);
    }

    @Bean
    public VerificarPrazosSagaUseCase verificarPrazosSagaUseCase(SagaOrdemServicoRepository sagas,
                                                                 PublicadorMensagens publicador,
                                                                 MetricasSaga metricas,
                                                                 UnidadeDeTrabalho u,
                                                                 ConfiguracaoSaga configuracao,
                                                                 Clock relogio) {
        return new VerificarPrazosSagaUseCase(sagas, publicador, metricas, u, configuracao, relogio);
    }

    @Bean
    public ConsultarSagaUseCase consultarSagaUseCase(SagaOrdemServicoRepository sagas) {
        return new ConsultarSagaUseCase(sagas);
    }

    @Bean
    public ConsultarHistoricoOSUseCase consultarHistoricoOSUseCase(OrdemServicoRepository os,
                                                                   HistoricoStatusRepository historico) {
        return new ConsultarHistoricoOSUseCase(os, historico);
    }

    @Bean
    public ConsultarIdentidadeUseCase consultarIdentidadeUseCase(ClienteRepository c, FuncionarioRepository f) {
        return new ConsultarIdentidadeUseCase(c, f);
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
