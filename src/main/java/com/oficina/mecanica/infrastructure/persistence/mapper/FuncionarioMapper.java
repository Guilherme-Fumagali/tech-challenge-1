package com.oficina.mecanica.infrastructure.persistence.mapper;

import com.oficina.mecanica.domain.entity.Funcionario;
import com.oficina.mecanica.infrastructure.persistence.entity.FuncionarioJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FuncionarioMapper {

    Funcionario toDomain(FuncionarioJpaEntity entity);
}
