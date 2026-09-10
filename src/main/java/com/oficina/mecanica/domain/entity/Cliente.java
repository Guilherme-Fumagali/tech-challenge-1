package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.domain.valueobject.StatusCliente;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class Cliente {

    private UUID id;
    private CpfCnpj cpfCnpj;
    private String nome;
    private String email;
    private String telefone;

    @Setter
    private StatusCliente status = StatusCliente.ATIVO;

    public Cliente(UUID id, CpfCnpj cpfCnpj, String nome, String email, String telefone) {
        this.id = id;
        this.cpfCnpj = cpfCnpj;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    public void atualizar(String nome, String email, String telefone) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }
}
