package com.example.airecommendations.dto;

import java.util.List;

public record RecommendationResponse(
        List<String> recommendations,
        String source,
        boolean fallback
) {}