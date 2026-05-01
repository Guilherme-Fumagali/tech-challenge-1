package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.Placa;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class Veiculo {

    private UUID id;
    private Placa placa;
    private String marca;
    private String modelo;
    private int anoFabricacao;
    private UUID clienteId;

    public void atualizar(String marca, String modelo, int anoFabricacao) {
        this.marca = marca;
        this.modelo = modelo;
        this.anoFabricacao = anoFabricacao;
    }
}
