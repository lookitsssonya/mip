package com.example.airecommendations.llm;

public record LlmRequest(String prompt, int maxTokens, double temperature) {

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String prompt;
        private int maxTokens = 500;
        private double temperature = 0.5;

        public Builder prompt(String prompt) { this.prompt = prompt; return this; }
        public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder temperature(double temperature) { this.temperature = temperature; return this; }
        public LlmRequest build() {
            if (prompt == null || prompt.isBlank()) {
                throw new IllegalArgumentException("prompt must not be blank");
            }
            return new LlmRequest(prompt, maxTokens, temperature);
        }
    }
}