package com.example.airecommendations.llm;

import com.example.airecommendations.config.LlmProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiGatewayTest {

    private LlmProperties properties;
    private AiGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties();
        properties.setActiveProvider("openai");
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        LlmClient openai = new LlmClient() {
            @Override public String providerName() { return "openai"; }
            @Override public LlmResponse generate(LlmRequest request) {
                return LlmResponse.of("openai response", "openai");
            }
        };
        LlmClient internal = new LlmClient() {
            @Override public String providerName() { return "internal"; }
            @Override public LlmResponse generate(LlmRequest request) {
                return LlmResponse.of("internal response", "internal");
            }
        };
        gateway = new AiGateway(List.of(openai, internal), properties, registry);
    }

    @Test
    void shouldRouteToActiveProvider() {
        LlmResponse response = gateway.generate(
                LlmRequest.builder().prompt("test").build());
        assertThat(response.text()).isEqualTo("openai response");
        assertThat(response.provider()).isEqualTo("openai");
    }

    @Test
    void shouldFallbackToAnotherProviderOnError() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LlmClient failing = new LlmClient() {
            @Override public String providerName() { return "openai"; }
            @Override public LlmResponse generate(LlmRequest request) {
                throw new RuntimeException("boom");
            }
        };
        LlmClient internal = new LlmClient() {
            @Override public String providerName() { return "internal"; }
            @Override public LlmResponse generate(LlmRequest request) {
                return LlmResponse.of("internal fallback", "internal");
            }
        };
        AiGateway g = new AiGateway(List.of(failing, internal), properties, registry);
        LlmResponse response = g.generate(LlmRequest.builder().prompt("test").build());

        assertThat(response.text()).isEqualTo("internal fallback");
        assertThat(response.provider()).isEqualTo("internal");
        assertThat(response.fallback()).isTrue();
    }

    @Test
    void shouldThrowOnUnknownProvider() {
        properties.setActiveProvider("unknown");
        assertThatThrownBy(() -> gateway.generate(
                LlmRequest.builder().prompt("test").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown LLM provider");
    }
}