package com.example.airecommendations.service;

import com.example.airecommendations.client.ProductCatalogClient;
import com.example.airecommendations.domain.Product;
import com.example.airecommendations.dto.ProductDto;
import com.example.airecommendations.dto.RecommendationResponse;
import com.example.airecommendations.llm.AiGateway;
import com.example.airecommendations.llm.LlmRequest;
import com.example.airecommendations.llm.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductRecommendationServiceTest {

    @Mock
    private AiGateway aiGateway;

    @Mock
    private ProductCatalogClient catalogClient;

    @InjectMocks
    private ProductRecommendationService service;

    @BeforeEach
    void setUp() {
        lenient().when(catalogClient.findSimilar(anyString()))
                .thenReturn(List.of());
    }

    @Test
    void shouldReturnParsedRecommendations() {
        when(aiGateway.generate(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of(
                        "Кофейные зерна\nСредство для накипи\nНабор чашек",
                        "openai"));

        ProductDto product = new ProductDto(
                "Кофемашина", "Бытовая техника", "Автоматическая", 45990.0);

        RecommendationResponse response = service.recommend(product);

        assertThat(response.recommendations())
                .containsExactly("Кофейные зерна", "Средство для накипи", "Набор чашек");
        assertThat(response.source()).isEqualTo("openai");
        assertThat(response.fallback()).isFalse();
    }

    @Test
    void shouldLimitRecommendationsToFive() {
        when(aiGateway.generate(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of("1\n2\n3\n4\n5\n6\n7", "openai"));

        ProductDto product = new ProductDto("X", "Y", null, null);
        RecommendationResponse response = service.recommend(product);

        assertThat(response.recommendations()).hasSize(5);
    }

    @Test
    void shouldThrowWhenNameIsBlank() {
        ProductDto product = new ProductDto("", "Category", null, null);

        assertThatThrownBy(() -> service.recommend(product))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void shouldThrowWhenCategoryIsBlank() {
        ProductDto product = new ProductDto("Name", " ", null, null);

        assertThatThrownBy(() -> service.recommend(product))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");
    }

    @Test
    void shouldReturnFallbackResponseFromGateway() {
        when(aiGateway.generate(any(LlmRequest.class)))
                .thenReturn(LlmResponse.fallback("Не удалось сгенерировать рекомендации."));

        ProductDto product = new ProductDto("Name", "Category", null, null);
        RecommendationResponse response = service.recommend(product);

        assertThat(response.fallback()).isTrue();
        assertThat(response.source()).isEqualTo("fallback");
    }

    @Test
    void shouldSanitizeLeadingMarkers() {
        when(aiGateway.generate(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of("- Товар 1\n* Товар 2\n1. Товар 3", "openai"));

        ProductDto product = new ProductDto("Name", "Category", null, null);
        RecommendationResponse response = service.recommend(product);

        assertThat(response.recommendations())
                .containsExactly("Товар 1", "Товар 2", "Товар 3");
    }

    @Test
    void shouldIncludeCatalogDataInPrompt() {
        when(catalogClient.findSimilar("Бытовая техника"))
                .thenReturn(List.of(new Product("Кофейные зерна", "Кофе", null, 990.0)));
        when(aiGateway.generate(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of("Товар 1", "openai"));

        ProductDto product = new ProductDto("Кофемашина", "Бытовая техника", null, 45990.0);
        service.recommend(product);

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(aiGateway).generate(captor.capture());

        assertThat(captor.getValue().prompt())
                .contains("Кофейные зерна")
                .contains("Кофемашина");
        }
}