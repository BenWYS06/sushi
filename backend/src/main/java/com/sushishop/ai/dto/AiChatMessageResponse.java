package com.sushishop.ai.dto;

import com.sushishop.ai.ChatRole;

import java.time.LocalDateTime;

public record AiChatMessageResponse(
        Long id,
        ChatRole role,
        String content,
        LocalDateTime createdAt
) {
}
