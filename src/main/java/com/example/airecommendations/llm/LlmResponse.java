package com.example.airecommendations.llm;

public record LlmResponse(String text, String provider, boolean fallback) {

    public static LlmResponse of(String text, String provider) {
        return new LlmResponse(text, provider, false);
    }

    public static LlmResponse fallback(String text) {
        return new LlmResponse(text, "fallback", true);
    }
}