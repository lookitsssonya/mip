package com.example.airecommendations.controller;

import com.example.airecommendations.dto.ProductDto;
import com.example.airecommendations.dto.RecommendationResponse;
import com.example.airecommendations.service.ProductRecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductRecommendationController {

    private final ProductRecommendationService service;

    public ProductRecommendationController(ProductRecommendationService service) {
        this.service = service;
    }

    @PostMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> recommend(@Valid @RequestBody ProductDto product) {
        return ResponseEntity.ok(service.recommend(product));
    }
}