package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.valueobject.StatusCliente;

import java.util.UUID;

public record ClienteResponse(
    UUID id,
    String cpfCnpj,
    String nome,
    String email,
    String telefone,
    StatusCliente status
) {
    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(c.getId(), c.getCpfCnpj().getValor(),
            c.getNome(), c.getEmail(), c.getTelefone(), c.getStatus());
    }
}
