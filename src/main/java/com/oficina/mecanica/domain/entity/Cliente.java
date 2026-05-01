package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.CpfCnpj;

import java.util.UUID;

public class Cliente {

    private UUID id;
    private CpfCnpj cpfCnpj;
    private String nome;
    private String email;
    private String telefone;

    public Cliente(UUID id, CpfCnpj cpfCnpj, String nome, String email, String telefone) {
        this.id = id;
        this.cpfCnpj = cpfCnpj;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    public UUID getId()         { return id; }
    public CpfCnpj getCpfCnpj(){ return cpfCnpj; }
    public String getNome()     { return nome; }
    public String getEmail()    { return email; }
    public String getTelefone() { return telefone; }

    public void atualizar(String nome, String email, String telefone) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }
}
