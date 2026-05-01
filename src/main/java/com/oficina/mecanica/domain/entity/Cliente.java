package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class Cliente {

    private UUID id;
    private CpfCnpj cpfCnpj;
    private String nome;
    private String email;
    private String telefone;

    public void atualizar(String nome, String email, String telefone) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }
}
