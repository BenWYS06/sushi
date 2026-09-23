package com.sushishop.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DispatchOrderRequest(

        @NotNull(message = "Courier ID is required")
        @Positive(message = "Courier ID must be positive")
        Long courierId
) {
}