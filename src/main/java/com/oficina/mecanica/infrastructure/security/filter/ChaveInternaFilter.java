package com.oficina.mecanica.infrastructure.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class ChaveInternaFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Chave-Interna";
    static final String PREFIXO_INTERNO = "/internal/";

    private final byte[] chaveEsperada;
    private final ObjectMapper objectMapper;

    public ChaveInternaFilter(String chaveInterna, ObjectMapper objectMapper) {
        this.chaveEsperada = chaveInterna == null ? new byte[0] : chaveInterna.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIXO_INTERNO);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (chaveValida(request.getHeader(CABECALHO))) {
            filterChain.doFilter(request, response);
            return;
        }
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credencial interna ausente ou inválida.");
        problema.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problema);
    }

    private boolean chaveValida(String recebida) {
        if (chaveEsperada.length == 0 || recebida == null || recebida.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(chaveEsperada, recebida.getBytes(StandardCharsets.UTF_8));
    }
}
