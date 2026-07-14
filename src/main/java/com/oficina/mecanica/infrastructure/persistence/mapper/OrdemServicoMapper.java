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

    // O dono do relacionamento no JPA é o item (@ManyToOne / ordem_servico_id), não a OS.
    // O MapStruct constrói cada item isoladamente e não tem como saber quem é o pai, então a
    // referência de volta é fechada aqui. Sem isto o insert quebra com ordem_servico_id nulo.
    @AfterMapping
    default void vincularItensAoPai(@MappingTarget OrdemServicoJpaEntity entity) {
        entity.getItensServico().forEach(item -> item.setOrdemServico(entity));
        entity.getItensPeca().forEach(item -> item.setOrdemServico(entity));
    }

    @Mapping(target = "ordemServico", ignore = true)
    ItemServicoJpaEntity toEntity(ItemServico item);

    @Mapping(target = "ordemServico", ignore = true)
    ItemPecaJpaEntity toEntity(ItemPeca item);

    // OrdemServico é Aggregate Root e não expõe construtor completo: quem remonta uma OS a
    // partir do banco é a própria factory do domínio. O MapStruct monta o DadosOrdemServico e
    // para por aí — a decisão de como reconstituir continua dentro do domínio, não no mapper.
    default OrdemServico toDomain(OrdemServicoJpaEntity entity) {
        return entity == null ? null : OrdemServico.reconstituir(toDados(entity));
    }

    DadosOrdemServico toDados(OrdemServicoJpaEntity entity);

    ItemServico toDomain(ItemServicoJpaEntity entity);

    ItemPeca toDomain(ItemPecaJpaEntity entity);
}
