package com.oficina.mecanica.application.usecase.identidade;

import java.util.UUID;

public record Identidade(UUID id, String nome, String email, String status) {}
