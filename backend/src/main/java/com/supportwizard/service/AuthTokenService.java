package com.supportwizard.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthTokenService {

    private final Set<String> chatTokens = ConcurrentHashMap.newKeySet();
    private final Set<String> knowledgeTokens = ConcurrentHashMap.newKeySet();

    public String gerarTokenChat() {
        String token = UUID.randomUUID().toString();
        chatTokens.add(token);
        return token;
    }

    public String gerarTokenKnowledge() {
        String token = UUID.randomUUID().toString();
        knowledgeTokens.add(token);
        return token;
    }

    public boolean isChatTokenValido(String token) {
        return token != null && chatTokens.contains(token);
    }

    public boolean isKnowledgeTokenValido(String token) {
        return token != null && knowledgeTokens.contains(token);
    }
}
