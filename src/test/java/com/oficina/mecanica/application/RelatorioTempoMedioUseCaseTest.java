package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.relatorio.RelatorioTempoMedioUseCase;
import com.oficina.mecanica.domain.entity.DadosOrdemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioTempoMedioUseCaseTest {

    @Mock OrdemServicoRepository repository;

    RelatorioTempoMedioUseCase useCase;

    private final LocalDateTime inicio = LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0);
    private final LocalDateTime fim    = LocalDateTime.of(2026, Month.DECEMBER, 31, 23, 59);

    @BeforeEach
    void setUp() {
        useCase = new RelatorioTempoMedioUseCase(repository);
    }

    private OrdemServico osComDatas(LocalDateTime dataInicio, LocalDateTime dataConclusao) {
        return OrdemServico.reconstituir(DadosOrdemServico.builder()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(StatusOS.FINALIZADA).dataAbertura(LocalDateTime.now())
            .dataInicio(dataInicio).dataConclusao(dataConclusao).build());
    }

    @Test
    void executar_deveRetornarZeroQuandoNaoHaOrdens() {
        when(repository.listarFinalizadasNoPeriodo(inicio, fim)).thenReturn(List.of());

        var resultado = useCase.executar(inicio, fim);

        assertThat(resultado.totalOrdens()).isZero();
        assertThat(resultado.tempoMedioHoras()).isZero();
    }

    @Test
    void executar_deveCalcularTempoMedioCorreto() {
        var dataInicio = LocalDateTime.of(2026, Month.MAY, 1, 8, 0);
        var dataConclusao = LocalDateTime.of(2026, Month.MAY, 1, 10, 0); // 2 horas
        var os = osComDatas(dataInicio, dataConclusao);
        when(repository.listarFinalizadasNoPeriodo(inicio, fim)).thenReturn(List.of(os));

        var resultado = useCase.executar(inicio, fim);

        assertThat(resultado.totalOrdens()).isEqualTo(1);
        assertThat(resultado.tempoMedioHoras()).isEqualTo(2.0);
    }

    @Test
    void executar_deveIgnorarOsSemDataInicioOuConclusao() {
        var osSemDatas = osComDatas(null, null);
        when(repository.listarFinalizadasNoPeriodo(inicio, fim)).thenReturn(List.of(osSemDatas));

        var resultado = useCase.executar(inicio, fim);

        assertThat(resultado.totalOrdens()).isEqualTo(1);
        assertThat(resultado.tempoMedioHoras()).isZero();
    }

    @Test
    void executar_deveCalcularMediaDeMultiplasOrdens() {
        var t1Inicio = LocalDateTime.of(2026, Month.MAY, 1, 8, 0);
        var t1Fim    = LocalDateTime.of(2026, Month.MAY, 1, 10, 0); // 2h
        var t2Inicio = LocalDateTime.of(2026, Month.MAY, 2, 8, 0);
        var t2Fim    = LocalDateTime.of(2026, Month.MAY, 2, 12, 0); // 4h
        when(repository.listarFinalizadasNoPeriodo(inicio, fim))
            .thenReturn(List.of(osComDatas(t1Inicio, t1Fim), osComDatas(t2Inicio, t2Fim)));

        var resultado = useCase.executar(inicio, fim);

        assertThat(resultado.totalOrdens()).isEqualTo(2);
        assertThat(resultado.tempoMedioHoras()).isEqualTo(3.0); // (2+4)/2
    }
}
