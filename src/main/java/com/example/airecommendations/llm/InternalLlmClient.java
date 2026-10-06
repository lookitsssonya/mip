package com.example.airecommendations.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InternalLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(InternalLlmClient.class);

    @Override
    public String providerName() {
        return "internal";
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        log.info("Internal LLM stub called, prompt length={}", request.prompt().length());

        List<String> recommendations = List.of(
                "Сопутствующий товар A (internal)",
                "Сопутствующий товар B (internal)",
                "Сопутствующий товар C (internal)"
        );
        String text = String.join("\n", recommendations);
        return LlmResponse.of(text, providerName());
    }
}