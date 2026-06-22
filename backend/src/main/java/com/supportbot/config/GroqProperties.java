package com.supportbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq.api")
public record GroqProperties(String url, String key, String model) {
}
