package com.example.airecommendations.service;

import com.example.airecommendations.client.ProductCatalogClient;
import com.example.airecommendations.domain.Product;
import com.example.airecommendations.dto.ProductDto;
import com.example.airecommendations.dto.RecommendationResponse;
import com.example.airecommendations.llm.AiGateway;
import com.example.airecommendations.llm.LlmRequest;
import com.example.airecommendations.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ProductRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ProductRecommendationService.class);
    private static final int MAX_RECOMMENDATIONS = 5;
    private static final int MAX_RESPONSE_LENGTH = 1000;

    private final AiGateway aiGateway;
    private final ProductCatalogClient catalogClient;

    public ProductRecommendationService(AiGateway aiGateway,
                                        ProductCatalogClient catalogClient) {
        this.aiGateway = aiGateway;
        this.catalogClient = catalogClient;
    }

    public RecommendationResponse recommend(ProductDto product) {
        validate(product);

        List<Product> similar = catalogClient.findSimilar(product.category());

        LlmRequest request = LlmRequest.builder()
                .prompt(buildPrompt(product, similar))
                .maxTokens(300)
                .temperature(0.4)
                .build();

        LlmResponse response = aiGateway.generate(request);
        List<String> recommendations = parseAndSanitize(response.text());
        return new RecommendationResponse(recommendations, response.provider(), response.fallback());
    }

    private void validate(ProductDto product) {
        if (product == null) {
            throw new IllegalArgumentException("Product must not be null");
        }
        if (product.name() == null || product.name().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (product.category() == null || product.category().isBlank()) {
            throw new IllegalArgumentException("Product category is required");
        }
    }

    private String buildPrompt(ProductDto product, List<Product> similar) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ты — ассистент интернет-магазина.\n");
        sb.append("Сгенерируй до ").append(MAX_RECOMMENDATIONS)
          .append(" сопутствующих товаров (cross-sell) для товара.\n");
        sb.append("Название: ").append(product.name()).append("\n");
        sb.append("Категория: ").append(product.category()).append("\n");
        sb.append("Описание: ")
          .append(product.description() == null ? "—" : product.description())
          .append("\n");
        sb.append("Цена: ")
          .append(product.price() == null ? "—" : product.price().toString())
          .append("\n");

        if (!similar.isEmpty()) {
            sb.append("Похожие товары из каталога:\n");
            similar.forEach(p -> sb.append("- ").append(p.name()).append("\n"));
        }

        sb.append("Требования:\n");
        sb.append("- каждый товар — отдельная строка;\n");
        sb.append("- без нумерации и маркеров;\n");
        sb.append("- только реальные сопутствующие товары;\n");
        sb.append("- не добавляй неподтверждённые характеристики.\n");
        return sb.toString();
    }

    private List<String> parseAndSanitize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String sanitized = text.length() > MAX_RESPONSE_LENGTH
                ? text.substring(0, MAX_RESPONSE_LENGTH)
                : text;
        return Arrays.stream(sanitized.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.replaceAll("^[\\-\\*\\d\\.\\)]+\\s*", ""))
                .limit(MAX_RECOMMENDATIONS)
                .toList();
    }
}