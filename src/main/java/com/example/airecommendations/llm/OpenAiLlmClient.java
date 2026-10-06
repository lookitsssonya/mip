package com.example.airecommendations.llm;

import com.example.airecommendations.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.example.airecommendations.exception.LlmException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final RestClient restClient;
    private final LlmProperties.Provider provider;
    private final RetryTemplate retryTemplate;

    public OpenAiLlmClient(RestClient.Builder builder, LlmProperties properties) {
        this.provider = properties.getProviders().get("openai");
        this.restClient = builder
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + safe(provider.getApiKey()))
                .build();
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(provider.getRetries() + 1)
                .fixedBackoff(Duration.ofMillis(500))
                .retryOn(Exception.class)
                .build();
    }

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        log.info("Calling OpenAI provider, prompt length={}, maxTokens={}",
                request.prompt().length(), request.maxTokens());
        try {
            return retryTemplate.execute(ctx -> {
                Map<String, Object> body = Map.of(
                        "model", provider.getModel(),
                        "messages", List.of(Map.of("role", "user", "content", request.prompt())),
                        "max_tokens", request.maxTokens(),
                        "temperature", request.temperature()
                );
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restClient.post()
                        .uri("/chat/completions")
                        .body(body)
                        .retrieve()
                        .body(Map.class);
                String text = extractText(response);
                return LlmResponse.of(text, providerName());
            });
        } catch (Exception e) {
            throw new LlmException("OpenAI call failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) return "";
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "";
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message == null ? "" : String.valueOf(message.getOrDefault("content", ""));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}