package com.example.airecommendations.llm;

public interface LlmClient {

    /**
     * @return уникальное имя провайдера (например, "openai", "internal")
     */
    String providerName();

    /**
     * Генерирует ответ на основании запроса.
     *
     * @param request запрос к LLM
     * @return ответ LLM
     * @throws LlmException при ошибке вызова провайдера
     */
    LlmResponse generate(LlmRequest request);
}