package com.sushishop.ai.dto;

import org.springframework.ai.tool.annotation.ToolParam;

public record DraftAddressInput(
        @ToolParam(description = "City") String city,
        @ToolParam(description = "Street") String street,
        @ToolParam(description = "House number") String house,
        @ToolParam(description = "Apartment number", required = false) String apartment,
        @ToolParam(description = "Delivery instructions", required = false) String comment
) {
}
