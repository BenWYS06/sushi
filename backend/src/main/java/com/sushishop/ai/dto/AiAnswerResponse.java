package com.sushishop.ai.dto;

import java.util.List;

public record AiAnswerResponse(
        String answer,
        List<AiSourceResponse> sources
) {
}
