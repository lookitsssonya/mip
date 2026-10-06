package com.example.airecommendations.controller;

import com.example.airecommendations.dto.ProductDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "llm.active-provider=internal"
})
class ProductRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnRecommendations() throws Exception {
        ProductDto product = new ProductDto(
                "Кофемашина", "Бытовая техника", "Автоматическая", 45990.0);

        mockMvc.perform(post("/api/v1/products/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("internal"))
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void shouldReturnBadRequestOnBlankName() throws Exception {
        ProductDto product = new ProductDto("", "Бытовая техника", null, null);

        mockMvc.perform(post("/api/v1/products/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest());
    }
}