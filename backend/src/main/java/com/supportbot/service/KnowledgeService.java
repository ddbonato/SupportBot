package com.supportbot.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class KnowledgeService {

    private static final String KNOWLEDGE_CSV = "classpath:knowledge.csv";

    private final ResourceLoader resourceLoader;

    public KnowledgeService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String getKnowledgeBase() {
        Resource resource = resourceLoader.getResource(KNOWLEDGE_CSV);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o arquivo knowledge.csv", e);
        }
    }
}
