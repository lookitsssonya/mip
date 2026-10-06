package com.example.airecommendations.domain;

public record Product(
        String name,
        String category,
        String description,
        Double price
) {}