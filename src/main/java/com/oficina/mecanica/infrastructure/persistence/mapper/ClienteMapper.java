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

    // O banco guarda o CPF/CNPJ como String; o domínio só aceita o value object, que valida
    // dígito verificador no construtor. A travessia da fronteira é aqui, e é de propósito:
    // uma linha corrompida no banco estoura na leitura, não lá na frente numa regra de negócio.
    default CpfCnpj toCpfCnpj(String valor) {
        return valor == null ? null : new CpfCnpj(valor);
    }

    default String toValor(CpfCnpj cpfCnpj) {
        return cpfCnpj == null ? null : cpfCnpj.getValor();
    }
}
