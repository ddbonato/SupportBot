package com.supportwizard.config;

import com.supportwizard.service.AuthTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    private final AuthTokenService authTokenService;

    public AdminAuthFilter(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("/api/admin/login".equals(path) || "/api/admin/knowledge/login".equals(path)) {
            return true;
        }
        return !path.startsWith("/api/admin") && !path.startsWith("/api/consulta");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        boolean tokenValido = path.startsWith("/api/consulta")
                ? isChatTokenValido(request)
                : isKnowledgeTokenValido(request);

        if (tokenValido) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"detail\":\"Não autorizado\"}");
    }

    private boolean isChatTokenValido(HttpServletRequest request) {
        return extrairToken(request)
                .map(authTokenService::isChatTokenValido)
                .orElse(false);
    }

    private boolean isKnowledgeTokenValido(HttpServletRequest request) {
        return extrairToken(request)
                .map(authTokenService::isKnowledgeTokenValido)
                .orElse(false);
    }

    private java.util.Optional<String> extrairToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return java.util.Optional.of(authorization.substring("Bearer ".length()).trim());
        }
        return java.util.Optional.empty();
    }
}
