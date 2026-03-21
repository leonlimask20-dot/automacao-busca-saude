package com.leonlima.automacao.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class BuscaDTO {

    @Data
    public static class Requisicao {
        @NotBlank(message = "O termo de busca é obrigatório")
        private String termo;

        private String sitioAlvo = "bula.fiocruz.br";
    }

    @Data
    @Builder
    public static class ItemResultado {
        private String titulo;
        private String descricao;
        private String url;
    }

    @Data
    @Builder
    public static class Resposta {
        private String termo;
        private String sitioAlvo;
        private int totalResultados;
        private List<ItemResultado> resultados;
        private long tempoExecucaoMs;
        private LocalDateTime executadoEm;
        private String status;      // SUCESSO ou ERRO
        private String mensagemErro;
    }
}
