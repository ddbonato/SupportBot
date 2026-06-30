package com.supportwizard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição de login administrativo")
public record AdminLoginRequest(
        @NotBlank
        @Schema(description = "Senha de administrador")
        String senha
) {
}
