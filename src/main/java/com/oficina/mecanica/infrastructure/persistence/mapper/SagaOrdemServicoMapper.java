package com.oficina.mecanica.infrastructure.persistence.mapper;

import com.oficina.mecanica.domain.saga.Compensacao;
import com.oficina.mecanica.domain.saga.DadosSagaOrdemServico;
import com.oficina.mecanica.domain.saga.PassoSaga;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;
import com.oficina.mecanica.domain.saga.TipoComando;
import com.oficina.mecanica.infrastructure.persistence.entity.SagaOrdemServicoJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.entity.SagaPassoJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = ConversorDadosJson.class)
public interface SagaOrdemServicoMapper {

    SagaOrdemServicoJpaEntity toEntity(SagaOrdemServico saga);

    DadosSagaOrdemServico toDados(SagaOrdemServicoJpaEntity entity);

    PassoSaga toDomain(SagaPassoJpaEntity entity);

    default SagaOrdemServico toDomain(SagaOrdemServicoJpaEntity entity) {
        return entity == null ? null : SagaOrdemServico.reconstituir(toDados(entity));
    }

    default LocalDateTime emUtc(Instant instante) {
        return instante == null ? null : LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    default Instant deUtc(LocalDateTime dataHora) {
        return dataHora == null ? null : dataHora.toInstant(ZoneOffset.UTC);
    }

    default String compensacoesComoTexto(List<Compensacao> compensacoes) {
        return compensacoes == null ? "" : compensacoes.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    default List<Compensacao> textoComoCompensacoes(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(",")).map(String::trim).map(Compensacao::valueOf).toList();
    }

    default String comandoComoTexto(TipoComando comando) {
        return comando == null ? null : comando.nome();
    }

    default TipoComando textoComoComando(String texto) {
        return texto == null ? null : TipoComando.porNome(texto);
    }
}
