package com.supportwizard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição para criar ou editar item da base de conhecimento")
public record KnowledgeRequest(
        @NotBlank
        @Schema(description = "Descrição do problema", example = "Impressora não imprime")
        String problema,

        @NotBlank
        @Schema(description = "Solução ou informação", example = "1. Verificar se está ligada\n2. Reiniciar spooler")
        String solucao
) {
}
