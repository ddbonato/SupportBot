package com.supportwizard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de login administrativo")
public record AdminLoginResponse(
        @Schema(description = "Token de acesso válido até reiniciar o backend")
        String token
) {
}
