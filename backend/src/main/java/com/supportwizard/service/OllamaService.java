package com.supportwizard.service;

import com.supportwizard.config.OllamaProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class OllamaService {

    private final WebClient ollamaWebClient;
    private final OllamaProperties ollamaProperties;

    public OllamaService(WebClient ollamaWebClient, OllamaProperties ollamaProperties) {
        this.ollamaWebClient = ollamaWebClient;
        this.ollamaProperties = ollamaProperties;
    }

    public String gerarResposta(String prompt) {
        var request = new OllamaGenerateRequest(ollamaProperties.model(), prompt, false);

        try {
            OllamaGenerateResponse response = ollamaWebClient.post()
                    .uri(ollamaProperties.url())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaGenerateResponse.class)
                    .block();

            if (response == null || response.response() == null || response.response().isBlank()) {
                throw new IllegalStateException("Resposta vazia da Ollama API");
            }

            return response.response();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Erro ao chamar a Ollama API: " + e.getResponseBodyAsString(), e);
        }
    }

    private record OllamaGenerateRequest(String model, String prompt, boolean stream) {
    }

    private record OllamaGenerateResponse(String response) {
    }
}
