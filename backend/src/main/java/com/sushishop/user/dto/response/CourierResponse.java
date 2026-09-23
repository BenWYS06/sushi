package com.sushishop.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Courier available for order dispatch")
public record CourierResponse(

        @Schema(description = "Courier user ID", example = "3")
        Long id,

        @Schema(description = "Courier name", example = "Ivan Petrenko")
        String name,

        @Schema(description = "Courier email", example = "courier@example.com")
        String email,

        @Schema(description = "Courier phone", example = "+380501234567")
        String phone
) {
}