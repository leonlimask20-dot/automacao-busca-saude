package com.leonlima.automacao.controller;

import com.leonlima.automacao.dto.BuscaDTO;
import com.leonlima.automacao.servico.ServicoAutomacao;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/busca")
@RequiredArgsConstructor
public class ControladorBusca {

    private final ServicoAutomacao servicoAutomacao;

    /**
     * POST /api/busca
     * Dispara a automação com Selenium e retorna os resultados como JSON.
     *
     * Corpo esperado:
     * {
     *   "termo": "paracetamol",
     *   "sitioAlvo": "bula.fiocruz.br"
     * }
     */
    @PostMapping
    public ResponseEntity<BuscaDTO.Resposta> buscar(@Valid @RequestBody BuscaDTO.Requisicao requisicao) {
        return ResponseEntity.ok(servicoAutomacao.buscar(requisicao));
    }
}
