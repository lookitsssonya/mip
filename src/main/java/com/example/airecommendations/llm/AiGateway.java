package com.example.airecommendations.llm;

import com.example.airecommendations.config.LlmProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiGateway {

    private static final Logger log = LoggerFactory.getLogger(AiGateway.class);

    private final Map<String, LlmClient> clients;
    private final LlmProperties properties;
    private final MeterRegistry meterRegistry;

    public AiGateway(List<LlmClient> clients,
                     LlmProperties properties,
                     MeterRegistry meterRegistry) {
        this.clients = clients.stream()
                .collect(Collectors.toMap(LlmClient::providerName, Function.identity()));
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public LlmResponse generate(LlmRequest request) {
        String active = properties.getActiveProvider();
        LlmClient client = clients.get(active);
        if (client == null) {
            throw new IllegalArgumentException("Unknown LLM provider: " + active);
        }
        try {
            LlmResponse response = client.generate(request);
            meterRegistry.counter("llm.requests",
                    "provider", client.providerName(),
                    "status", "success").increment();
            log.info("LLM request successful, provider={}, fallback={}",
                    client.providerName(), response.fallback());
            return response;
        } catch (Exception e) {
            meterRegistry.counter("llm.requests",
                    "provider", client.providerName(),
                    "status", "error").increment();
            log.warn("LLM request failed, provider={}, reason={}",
                    client.providerName(), e.getMessage());
            return fallback(request, e);
        }
    }

    private LlmResponse fallback(LlmRequest request, Exception original) {
    String active = properties.getActiveProvider();
    return clients.values().stream()
            .filter(c -> !c.providerName().equals(active))
            .findFirst()
            .map(alt -> {
                try {
                    log.info("Trying fallback provider={}", alt.providerName());
                    LlmResponse response = alt.generate(request);
                    meterRegistry.counter("llm.requests",
                            "provider", alt.providerName(),
                            "status", "fallback-success").increment();
                    return new LlmResponse(response.text(), response.provider(), true);
                } catch (Exception e) {
                    log.warn("Fallback provider={} also failed: {}",
                            alt.providerName(), e.getMessage());
                    return null;
                }
            })
            .orElseGet(() -> {
                meterRegistry.counter("llm.requests",
                        "provider", active,
                        "status", "fallback-default").increment();
                return LlmResponse.fallback(
                        "Не удалось сгенерировать рекомендации. Попробуйте позже.");
            });
    }
}