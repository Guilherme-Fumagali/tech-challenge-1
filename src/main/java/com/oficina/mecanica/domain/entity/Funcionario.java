package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.StatusFuncionario;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class Funcionario {

    private UUID id;
    private String cpf;
    private String nome;
    private String email;
    private StatusFuncionario status;
}
