package com.supportbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição de consulta de suporte técnico")
public record ConsultaRequest(
        @NotBlank
        @Schema(
                description = "Descrição do problema relatado pelo técnico",
                example = "usuário não consegue conectar na VPN após atualização"
        )
        String problema
) {
}
