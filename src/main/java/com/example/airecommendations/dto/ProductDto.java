package com.example.airecommendations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductDto(
        @NotBlank(message = "Product name is required")
        @Size(max = 200)
        String name,

        @NotBlank(message = "Category is required")
        @Size(max = 200)
        String category,

        @Size(max = 2000)
        String description,

        @Positive
        Double price
) {}