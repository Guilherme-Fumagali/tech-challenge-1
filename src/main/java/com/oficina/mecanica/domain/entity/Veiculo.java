package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.Placa;

import java.util.UUID;

public class Veiculo {

    private UUID id;
    private Placa placa;
    private String marca;
    private String modelo;
    private int anoFabricacao;
    private UUID clienteId;

    public Veiculo(UUID id, Placa placa, String marca, String modelo, int anoFabricacao, UUID clienteId) {
        this.id = id;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.anoFabricacao = anoFabricacao;
        this.clienteId = clienteId;
    }

    public UUID getId()           { return id; }
    public Placa getPlaca()       { return placa; }
    public String getMarca()      { return marca; }
    public String getModelo()     { return modelo; }
    public int getAnoFabricacao() { return anoFabricacao; }
    public UUID getClienteId()    { return clienteId; }

    public void atualizar(String marca, String modelo, int anoFabricacao) {
        this.marca = marca;
        this.modelo = modelo;
        this.anoFabricacao = anoFabricacao;
    }
}
