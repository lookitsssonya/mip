package com.example.airecommendations.client;

import com.example.airecommendations.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogClient.class);

    public List<Product> findSimilar(String category) {
        log.info("ProductCatalogClient.findSimilar called for category={}", category);
        return List.of();
    }
}