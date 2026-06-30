package com.supportwizard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama.api")
public record OllamaProperties(String url, String model) {
}
