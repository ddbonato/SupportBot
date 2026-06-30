package com.supportwizard.controller;

import com.supportwizard.dto.ConsultaRequest;
import com.supportwizard.dto.ConsultaResponse;
import com.supportwizard.dto.FeedbackRequest;
import com.supportwizard.service.ConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Consulta", description = "Consultas de suporte técnico e feedback das respostas")
@RestController
@RequestMapping("/api/consulta")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @Operation(
            summary = "Enviar problema para análise",
            description = "Recebe a descrição do problema, consulta a base de conhecimento via IA e retorna a solução sugerida"
    )
    @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso")
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
    @ApiResponse(responseCode = "500", description = "Erro interno ao processar a consulta")
    @PostMapping
    public ConsultaResponse consultar(@Valid @RequestBody ConsultaRequest request) {
        return consultaService.processarConsulta(request.problema());
    }

    @Operation(
            summary = "Registrar feedback da resposta",
            description = "Atualiza se a resposta da IA foi útil para o técnico de suporte"
    )
    @ApiResponse(responseCode = "204", description = "Feedback registrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
    @ApiResponse(responseCode = "404", description = "Consulta não encontrada")
    @PatchMapping("/{id}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarFeedback(
            @Parameter(description = "ID da consulta") @PathVariable Long id,
            @Valid @RequestBody FeedbackRequest request) {
        consultaService.registrarFeedback(id, request.util());
    }
}
