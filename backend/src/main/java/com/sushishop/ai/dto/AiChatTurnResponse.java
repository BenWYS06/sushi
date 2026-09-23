package com.sushishop.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiChatTurnResponse(
        UUID sessionId,
        String answer,
        List<AiSourceResponse> sources,
        AiOrderDraftResponse draft
) {
}
