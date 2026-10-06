package com.example.airecommendations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String activeProvider = "internal";
    private Duration defaultTimeout = Duration.ofSeconds(5);
    private int defaultRetries = 2;
    private Map<String, Provider> providers = new HashMap<>();

    public static class Provider {
        private String baseUrl;
        private String apiKey;
        private String model;
        private Duration timeout = Duration.ofSeconds(5);
        private int retries = 2;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        public int getRetries() { return retries; }
        public void setRetries(int retries) { this.retries = retries; }
    }

    public String getActiveProvider() { return activeProvider; }
    public void setActiveProvider(String activeProvider) { this.activeProvider = activeProvider; }
    public Duration getDefaultTimeout() { return defaultTimeout; }
    public void setDefaultTimeout(Duration defaultTimeout) { this.defaultTimeout = defaultTimeout; }
    public int getDefaultRetries() { return defaultRetries; }
    public void setDefaultRetries(int defaultRetries) { this.defaultRetries = defaultRetries; }
    public Map<String, Provider> getProviders() { return providers; }
    public void setProviders(Map<String, Provider> providers) { this.providers = providers; }
}