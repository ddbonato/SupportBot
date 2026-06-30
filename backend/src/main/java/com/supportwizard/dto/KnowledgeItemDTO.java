package com.supportwizard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item da base de conhecimento")
public record KnowledgeItemDTO(
        @Schema(description = "Índice do item na base (0-based)") int indice,
        @Schema(description = "Descrição do problema") String problema,
        @Schema(description = "Solução ou informação associada") String solucao
) {
}
