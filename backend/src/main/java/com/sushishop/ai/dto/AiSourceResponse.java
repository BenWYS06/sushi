package com.sushishop.ai.dto;

public record AiSourceResponse(
        String type,
        String sourceId,
        String title,
        String url,
        Double score,
        String content
) {
}
