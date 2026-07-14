package com.oficina.mecanica.infrastructure.persistence.mapper;

import com.oficina.mecanica.domain.entity.Servico;
import com.oficina.mecanica.infrastructure.persistence.entity.ServicoJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ServicoMapper {

    ServicoJpaEntity toEntity(Servico servico);

    Servico toDomain(ServicoJpaEntity entity);
}
