package com.supportwizard.controller;

import com.supportwizard.dto.AdminLoginRequest;
import com.supportwizard.dto.AdminLoginResponse;
import com.supportwizard.service.AuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Auth", description = "Autenticação do chat e da base de conhecimento")
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AuthTokenService authTokenService;
    private final String chatSenha;
    private final String knowledgeSenha;

    public AdminAuthController(
            AuthTokenService authTokenService,
            @Value("${chat.senha}") String chatSenha,
            @Value("${knowledge.senha}") String knowledgeSenha) {
        this.authTokenService = authTokenService;
        this.chatSenha = chatSenha;
        this.knowledgeSenha = knowledgeSenha;
    }

    @Operation(summary = "Login do chat")
    @ApiResponse(responseCode = "200", description = "Login realizado com sucesso")
    @ApiResponse(responseCode = "401", description = "Senha incorreta")
    @PostMapping("/login")
    public AdminLoginResponse loginChat(@Valid @RequestBody AdminLoginRequest request) {
        if (!chatSenha.equals(request.senha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta");
        }
        return new AdminLoginResponse(authTokenService.gerarTokenChat());
    }

    @Operation(summary = "Login da base de conhecimento")
    @ApiResponse(responseCode = "200", description = "Login realizado com sucesso")
    @ApiResponse(responseCode = "401", description = "Senha incorreta")
    @PostMapping("/knowledge/login")
    public AdminLoginResponse loginKnowledge(@Valid @RequestBody AdminLoginRequest request) {
        if (!knowledgeSenha.equals(request.senha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha incorreta");
        }
        return new AdminLoginResponse(authTokenService.gerarTokenKnowledge());
    }
}
