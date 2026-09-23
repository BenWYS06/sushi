package com.sushishop.ai.dto;

import java.math.BigDecimal;

public record AiOrderDraftItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
