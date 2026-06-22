package com.supportbot.service;

import com.supportbot.config.GroqProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Service
public class GroqService {

    private final WebClient groqWebClient;
    private final GroqProperties groqProperties;

    public GroqService(WebClient groqWebClient, GroqProperties groqProperties) {
        this.groqWebClient = groqWebClient;
        this.groqProperties = groqProperties;
    }

    public String gerarResposta(String prompt) {
        var request = new GroqChatRequest(
                groqProperties.model(),
                List.of(new GroqMessage("user", prompt))
        );

        try {
            GroqChatResponse response = groqWebClient.post()
                    .uri(groqProperties.url())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GroqChatResponse.class)
                    .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("Resposta vazia da Groq API");
            }

            return response.choices().get(0).message().content();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Erro ao chamar a Groq API: " + e.getResponseBodyAsString(), e);
        }
    }

    private record GroqChatRequest(String model, List<GroqMessage> messages) {
    }

    private record GroqMessage(String role, String content) {
    }

    private record GroqChatResponse(List<Choice> choices) {
    }

    private record Choice(GroqMessage message) {
    }
}
