package com.oficina.mecanica.infrastructure;

import com.oficina.mecanica.DocumentosDeTeste;
import com.oficina.mecanica.application.port.UnidadeDeTrabalho;
import com.oficina.mecanica.application.usecase.ordemservico.AdicionarPecaAOSUseCase;
import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.entity.ItemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.entity.Servico;
import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.PecaRepository;
import com.oficina.mecanica.domain.repository.ServicoRepository;
import com.oficina.mecanica.domain.repository.VeiculoRepository;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.domain.valueobject.Placa;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class UnidadeDeTrabalhoIntegrationTest extends IntegracaoBase {

    @Autowired UnidadeDeTrabalho unidadeDeTrabalho;
    @Autowired OrdemServicoRepository osRepository;
    @Autowired PecaRepository pecaRepository;
    @Autowired ServicoRepository servicoRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired VeiculoRepository veiculoRepository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void falhaDepoisDaBaixaDeEstoqueDesfazABaixa() {
        var osId = ordemEmDiagnostico();
        var pecaId = peca(10);
        var osQueFalhaAoGravar = spy(osRepository);
        doThrow(new IllegalStateException("falha simulada ao gravar a OS")).when(osQueFalhaAoGravar).salvar(any());
        var useCase = new AdicionarPecaAOSUseCase(osQueFalhaAoGravar, pecaRepository, unidadeDeTrabalho);

        assertThatThrownBy(() -> useCase.executar(osId, pecaId, 3))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("falha simulada");

        assertThat(estoque(pecaId)).isEqualTo(10);
        assertThat(itensPeca(osId)).isZero();
    }

    @Test
    void baixaDeEstoqueEItemDaOrdemSaoGravadosJuntos() {
        var osId = ordemEmDiagnostico();
        var pecaId = peca(10);
        var useCase = new AdicionarPecaAOSUseCase(osRepository, pecaRepository, unidadeDeTrabalho);

        useCase.executar(osId, pecaId, 3);

        assertThat(estoque(pecaId)).isEqualTo(7);
        assertThat(itensPeca(osId)).isEqualTo(1);
        assertThat(versao("pecas", pecaId)).isEqualTo(1L);
        assertThat(versao("ordens_servico", osId)).isEqualTo(1L);
    }

    @Test
    void gravacaoComVersaoDesatualizadaEhRecusada() {
        var pecaId = peca(10);
        var copiaDesatualizada = pecaRepository.buscarPorId(pecaId).orElseThrow();
        var copiaAtual = pecaRepository.buscarPorId(pecaId).orElseThrow();
        copiaAtual.incrementarEstoque(1);
        pecaRepository.salvar(copiaAtual);

        copiaDesatualizada.incrementarEstoque(5);

        assertThatThrownBy(() -> pecaRepository.salvar(copiaDesatualizada))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(estoque(pecaId)).isEqualTo(11);
    }

    @Test
    void mesmaOrdemGravadaDuasVezesNaMesmaUnidadeMantemAVersaoSincronizada() {
        var osId = ordemEmDiagnostico();
        var servico = servicoRepository.salvar(new Servico(UUID.randomUUID(), "Alinhamento", "",
            new BigDecimal("90.00"), BigDecimal.ONE));

        unidadeDeTrabalho.executar(() -> {
            var os = osRepository.buscarPorId(osId).orElseThrow();
            os.adicionarServico(new ItemServico(UUID.randomUUID(), servico.getId(), servico.getNome(),
                servico.getPrecoUnitario(), 1));
            osRepository.salvar(os);
            os.adicionarServico(new ItemServico(UUID.randomUUID(), servico.getId(), servico.getNome(),
                servico.getPrecoUnitario(), 2));
            osRepository.salvar(os);
        });

        assertThat(versao("ordens_servico", osId)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itens_servico WHERE ordem_servico_id = ?",
            Integer.class, osId)).isEqualTo(2);
    }

    private UUID ordemEmDiagnostico() {
        return unidadeDeTrabalho.executar(() -> {
            var cliente = clienteRepository.salvar(new Cliente(UUID.randomUUID(),
                new CpfCnpj(DocumentosDeTeste.cpfValido()), "Cliente da transação", null, null));
            var veiculo = veiculoRepository.salvar(new Veiculo(UUID.randomUUID(),
                new Placa(DocumentosDeTeste.placaValida()), "Fiat", "Uno", 2015, cliente.getId()));
            var os = new OrdemServico(UUID.randomUUID(), cliente.getId(), veiculo.getId());
            os.registrarInicioDiagnostico();
            osRepository.salvar(os);
            return os.getId();
        });
    }

    private UUID peca(int estoque) {
        var peca = new Peca(UUID.randomUUID(), "Correia dentada", "", new BigDecimal("120.00"), estoque, 1);
        pecaRepository.salvar(peca);
        return peca.getId();
    }

    private int estoque(UUID pecaId) {
        return jdbc.queryForObject("SELECT quantidade_estoque FROM pecas WHERE id = ?", Integer.class, pecaId);
    }

    private int itensPeca(UUID osId) {
        return jdbc.queryForObject("SELECT count(*) FROM itens_peca WHERE ordem_servico_id = ?", Integer.class, osId);
    }

    private long versao(String tabela, UUID id) {
        return jdbc.queryForObject("SELECT versao FROM " + tabela + " WHERE id = ?", Long.class, id);
    }
}
