package com.sushishop.ai.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AiChatSessionResponse(
        UUID id,
        LocalDateTime createdAt,
        List<AiChatMessageResponse> messages,
        AiOrderDraftResponse draft
) {
}
