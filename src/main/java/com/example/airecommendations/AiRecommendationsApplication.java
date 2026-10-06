package com.example.airecommendations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class AiRecommendationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRecommendationsApplication.class, args);
    }
}