package com.supportwizard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Feedback sobre a utilidade da resposta")
public record FeedbackRequest(
        @NotNull
        @Schema(description = "Indica se a resposta foi útil (true = 👍, false = 👎)", example = "true")
        Boolean util
) {
}
