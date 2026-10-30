package com.oficina.mecanica.infrastructure.persistence.mapper;

import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.infrastructure.persistence.entity.PecaJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PecaMapper {

    PecaJpaEntity toEntity(Peca peca);

    default Peca toDomain(PecaJpaEntity entity) {
        return entity == null ? null : Peca.reconstituir(entity.getId(), entity.getNome(), entity.getDescricao(),
            entity.getPrecoUnitario(), entity.getQuantidadeEstoque(), entity.getEstoqueMinimo(), entity.getVersao());
    }
}
