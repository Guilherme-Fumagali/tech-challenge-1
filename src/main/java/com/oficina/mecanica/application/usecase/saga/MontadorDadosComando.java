package com.oficina.mecanica.application.usecase.saga;

import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.VeiculoRepository;
import com.oficina.mecanica.domain.saga.ComandoSaga;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MontadorDadosComando {

    private static final String TIPO_SERVICO = "SERVICO";
    private static final String TIPO_PECA = "PECA";

    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;

    public MontadorDadosComando(ClienteRepository clienteRepository, VeiculoRepository veiculoRepository) {
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
    }

    public Map<String, Object> montar(ComandoSaga comando, OrdemServico os) {
        return switch (comando.tipo()) {
            case INICIAR_DIAGNOSTICO -> iniciarDiagnostico(os);
            case INICIAR_REPARO -> iniciarReparo(os);
            case GERAR_ORCAMENTO -> gerarOrcamento(os);
            case REABRIR_DIAGNOSTICO, CANCELAR_EXECUCAO, CANCELAR_ORCAMENTO, ESTORNAR_PAGAMENTO ->
                Map.of("motivo", Objects.requireNonNullElse(comando.motivo(), ""));
        };
    }

    private Map<String, Object> iniciarDiagnostico(OrdemServico os) {
        var veiculo = veiculo(os);
        var dadosVeiculo = new LinkedHashMap<String, Object>();
        dadosVeiculo.put("placa", veiculo.getPlaca().getValor());
        dadosVeiculo.put("marca", veiculo.getMarca());
        dadosVeiculo.put("modelo", veiculo.getModelo());
        dadosVeiculo.put("anoFabricacao", veiculo.getAnoFabricacao());
        var dados = new LinkedHashMap<String, Object>();
        dados.put("clienteId", os.getClienteId().toString());
        dados.put("veiculo", dadosVeiculo);
        dados.put("abertaEm", os.getDataAbertura().toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS).toString());
        return dados;
    }

    private Map<String, Object> iniciarReparo(OrdemServico os) {
        var itens = new ArrayList<Map<String, Object>>();
        os.getItensServico().forEach(item -> itens.add(itemReparo(TIPO_SERVICO, item.getNomeServico(), item.getQuantidade())));
        os.getItensPeca().forEach(item -> itens.add(itemReparo(TIPO_PECA, item.getNomePeca(), item.getQuantidade())));
        return Map.of("itens", itens);
    }

    private Map<String, Object> gerarOrcamento(OrdemServico os) {
        var cliente = cliente(os);
        var veiculo = veiculo(os);
        var dadosCliente = new LinkedHashMap<String, Object>();
        dadosCliente.put("nome", cliente.getNome());
        dadosCliente.put("email", cliente.getEmail());
        var dadosVeiculo = new LinkedHashMap<String, Object>();
        dadosVeiculo.put("placa", veiculo.getPlaca().getValor());
        dadosVeiculo.put("modelo", veiculo.getModelo());
        var itens = new ArrayList<Map<String, Object>>();
        os.getItensServico().forEach(item -> itens.add(itemOrcamento(TIPO_SERVICO, item.getServicoId().toString(),
            item.getNomeServico(), item.getQuantidade(), item.getPrecoSnapshot())));
        os.getItensPeca().forEach(item -> itens.add(itemOrcamento(TIPO_PECA, item.getPecaId().toString(),
            item.getNomePeca(), item.getQuantidade(), item.getPrecoSnapshot())));
        var dados = new LinkedHashMap<String, Object>();
        dados.put("clienteId", os.getClienteId().toString());
        dados.put("cliente", dadosCliente);
        dados.put("veiculo", dadosVeiculo);
        dados.put("itens", itens);
        return dados;
    }

    private static Map<String, Object> itemReparo(String tipo, String descricao, int quantidade) {
        var item = new LinkedHashMap<String, Object>();
        item.put("tipo", tipo);
        item.put("descricao", descricao);
        item.put("quantidade", quantidade);
        return item;
    }

    private static Map<String, Object> itemOrcamento(String tipo, String referenciaId, String descricao,
                                                     int quantidade, BigDecimal precoUnitario) {
        var item = new LinkedHashMap<String, Object>();
        item.put("tipo", tipo);
        item.put("referenciaId", referenciaId);
        item.put("descricao", descricao);
        item.put("quantidade", quantidade);
        item.put("precoUnitario", precoUnitario.setScale(2, RoundingMode.HALF_UP));
        return item;
    }

    private Cliente cliente(OrdemServico os) {
        return clienteRepository.buscarPorId(os.getClienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", os.getClienteId()));
    }

    private Veiculo veiculo(OrdemServico os) {
        return veiculoRepository.buscarPorId(os.getVeiculoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo", os.getVeiculoId()));
    }
}
