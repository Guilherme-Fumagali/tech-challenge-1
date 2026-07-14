package com.oficina.mecanica.infrastructure.persistence.mapper;

import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.valueobject.Placa;
import com.oficina.mecanica.infrastructure.persistence.entity.VeiculoJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VeiculoMapper {

    VeiculoJpaEntity toEntity(Veiculo veiculo);

    Veiculo toDomain(VeiculoJpaEntity entity);

    default Placa toPlaca(String valor) {
        return valor == null ? null : new Placa(valor);
    }

    default String toValor(Placa placa) {
        return placa == null ? null : placa.getValor();
    }
}
