package com.leonlima.automacao.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class TratadorDeExcecoes {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> errosCampos = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
                errosCampos.put(((FieldError) e).getField(), e.getDefaultMessage()));

        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "mensagem", "Erro de validação",
                "erros", errosCampos,
                "momento", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erroInterno(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "mensagem", "Erro interno: " + ex.getMessage(),
                "momento", LocalDateTime.now().toString()
        ));
    }
}
