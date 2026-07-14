package com.oficina.mecanica.infrastructure.persistence.mapper;

import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.infrastructure.persistence.entity.ClienteJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClienteMapper {

    ClienteJpaEntity toEntity(Cliente cliente);

    Cliente toDomain(ClienteJpaEntity entity);

    default CpfCnpj toCpfCnpj(String valor) {
        return valor == null ? null : new CpfCnpj(valor);
    }

    default String toValor(CpfCnpj cpfCnpj) {
        return cpfCnpj == null ? null : cpfCnpj.getValor();
    }
}
