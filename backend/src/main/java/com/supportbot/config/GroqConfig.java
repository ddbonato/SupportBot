package com.supportbot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(GroqProperties.class)
public class GroqConfig {

    @Bean
    public WebClient groqWebClient(WebClient.Builder builder, GroqProperties properties) {
        return builder
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.key())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
