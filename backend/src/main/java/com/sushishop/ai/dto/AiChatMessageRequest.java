package com.sushishop.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatMessageRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 1000, message = "Message must be less than 1000 characters")
        String message
) {
}
