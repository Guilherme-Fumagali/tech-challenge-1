package com.oficina.mecanica.infrastructure.persistence.mapper;

import com.oficina.mecanica.domain.entity.DadosOrdemServico;
import com.oficina.mecanica.domain.entity.ItemPeca;
import com.oficina.mecanica.domain.entity.ItemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.infrastructure.persistence.entity.ItemPecaJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.entity.ItemServicoJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.entity.OrdemServicoJpaEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrdemServicoMapper {

    OrdemServicoJpaEntity toEntity(OrdemServico os);

    @AfterMapping
    default void vincularItensAoPai(@MappingTarget OrdemServicoJpaEntity entity) {
        entity.getItensServico().forEach(item -> item.setOrdemServico(entity));
        entity.getItensPeca().forEach(item -> item.setOrdemServico(entity));
    }

    @Mapping(target = "ordemServico", ignore = true)
    ItemServicoJpaEntity toEntity(ItemServico item);

    @Mapping(target = "ordemServico", ignore = true)
    ItemPecaJpaEntity toEntity(ItemPeca item);

    default OrdemServico toDomain(OrdemServicoJpaEntity entity) {
        return entity == null ? null : OrdemServico.reconstituir(toDados(entity));
    }

    DadosOrdemServico toDados(OrdemServicoJpaEntity entity);

    ItemServico toDomain(ItemServicoJpaEntity entity);

    ItemPeca toDomain(ItemPecaJpaEntity entity);
}
