package com.supportwizard.controller;

import com.supportwizard.dto.KnowledgeItemDTO;
import com.supportwizard.dto.KnowledgeRequest;
import com.supportwizard.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Knowledge", description = "Gerenciamento da base de conhecimento")
@RestController
@RequestMapping("/api/admin/knowledge")
public class KnowledgeAdminController {

    private final KnowledgeService knowledgeService;

    public KnowledgeAdminController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Operation(summary = "Listar todos os casos da base de conhecimento")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public List<KnowledgeItemDTO> listarTodos() {
        return knowledgeService.listarTodos();
    }

    @Operation(summary = "Criar novo caso na base de conhecimento")
    @ApiResponse(responseCode = "201", description = "Caso criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeItemDTO criar(@Valid @RequestBody KnowledgeRequest request) {
        return knowledgeService.adicionar(request.problema(), request.solucao());
    }

    @Operation(summary = "Atualizar caso existente")
    @ApiResponse(responseCode = "200", description = "Caso atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Requisição inválida")
    @PutMapping("/{indice}")
    public KnowledgeItemDTO atualizar(
            @Parameter(description = "Índice do caso (0-based)") @PathVariable int indice,
            @Valid @RequestBody KnowledgeRequest request) {
        return knowledgeService.atualizar(indice, request.problema(), request.solucao());
    }

    @Operation(summary = "Excluir caso da base de conhecimento")
    @ApiResponse(responseCode = "204", description = "Caso excluído com sucesso")
    @ApiResponse(responseCode = "400", description = "Índice inválido")
    @DeleteMapping("/{indice}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @Parameter(description = "Índice do caso (0-based)") @PathVariable int indice) {
        knowledgeService.excluir(indice);
    }
}
