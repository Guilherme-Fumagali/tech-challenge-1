package com.oficina.mecanica.infrastructure.web.handler;

import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.EstoqueInsuficienteException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.exception.TokenAprovacaoInvalidoException;
import com.oficina.mecanica.domain.exception.TransicaoInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(RecursoNaoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ResponseEntity<ProblemDetail> handleTransicao(TransicaoInvalidaException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<ProblemDetail> handleEstoque(EstoqueInsuficienteException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TokenAprovacaoInvalidoException.class)
    public ResponseEntity<ProblemDetail> handleTokenAprovacao(TokenAprovacaoInvalidoException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        var erros = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, erros);
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String detail) {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(pd);
    }
}
