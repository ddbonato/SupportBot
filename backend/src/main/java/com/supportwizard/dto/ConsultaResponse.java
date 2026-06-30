package com.supportwizard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta da consulta de suporte técnico")
public record ConsultaResponse(
        @Schema(description = "Solução sugerida pela IA")
        String resposta,
        @Schema(description = "Identificador da consulta salva no histórico", example = "1")
        Long consultaId
) {
}
