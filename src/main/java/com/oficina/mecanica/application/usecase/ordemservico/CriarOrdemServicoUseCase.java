package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.application.port.ComandoAEnviar;
import com.oficina.mecanica.application.port.MetricasSaga;
import com.oficina.mecanica.application.port.PublicadorMensagens;
import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.application.usecase.saga.ConfiguracaoSaga;
import com.oficina.mecanica.application.usecase.saga.MontadorDadosComando;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.SagaOrdemServicoRepository;
import com.oficina.mecanica.domain.repository.VeiculoRepository;
import com.oficina.mecanica.domain.saga.ComandoSaga;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;
import com.oficina.mecanica.domain.saga.TipoComando;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class CriarOrdemServicoUseCase {

    private final OrdemServicoRepository osRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final SagaOrdemServicoRepository sagaRepository;
    private final MontadorDadosComando montadorDados;
    private final PublicadorMensagens publicador;
    private final MetricasSaga metricasSaga;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final ConfiguracaoSaga configuracao;
    private final Clock relogio;

    public CriarOrdemServicoUseCase(OrdemServicoRepository osRepository,
                                    ClienteRepository clienteRepository,
                                    VeiculoRepository veiculoRepository,
                                    SagaOrdemServicoRepository sagaRepository,
                                    MontadorDadosComando montadorDados,
                                    PublicadorMensagens publicador,
                                    MetricasSaga metricasSaga,
                                    UnidadeDeTrabalho unidadeDeTrabalho,
                                    ConfiguracaoSaga configuracao,
                                    Clock relogio) {
        this.osRepository = osRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.sagaRepository = sagaRepository;
        this.montadorDados = montadorDados;
        this.publicador = publicador;
        this.metricasSaga = metricasSaga;
        this.unidadeDeTrabalho = unidadeDeTrabalho;
        this.configuracao = configuracao;
        this.relogio = relogio;
    }

    public AberturaOrdemServico executar(UUID clienteId, UUID veiculoId) {
        var abertura = unidadeDeTrabalho.executar(() -> {
            var cliente = clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
            var veiculo = veiculoRepository.buscarPorId(veiculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo", veiculoId));

            if (!veiculo.getClienteId().equals(cliente.getId())) {
                throw new DomainException("O veículo não pertence ao cliente informado.");
            }

            var os = new OrdemServico(UUID.randomUUID(), clienteId, veiculoId);
            osRepository.salvar(os);

            var agora = Instant.now(relogio);
            var saga = SagaOrdemServico.iniciar(UUID.randomUUID(), os.getId(), agora);
            var dados = montadorDados.montar(new ComandoSaga(TipoComando.INICIAR_DIAGNOSTICO, null), os);
            var mensagemId = UUID.randomUUID();
            publicador.publicar(new ComandoAEnviar(mensagemId, TipoComando.INICIAR_DIAGNOSTICO, saga.getId(),
                os.getId(), null, dados));
            saga.registrarEnvio(mensagemId, TipoComando.INICIAR_DIAGNOSTICO, dados, agora,
                configuracao.prazoResposta());
            sagaRepository.salvar(saga);
            return new AberturaOrdemServico(os, saga.getId());
        });
        metricasSaga.registrarInicio(abertura.sagaId());
        return abertura;
    }
}
