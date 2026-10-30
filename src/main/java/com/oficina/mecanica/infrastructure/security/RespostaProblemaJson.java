package com.oficina.mecanica.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class RespostaProblemaJson {

    private RespostaProblemaJson() {}

    public static void escrever(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                                HttpStatus status, String detalhe) throws IOException {
        var problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
