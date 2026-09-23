package com.sushishop.ai.dto;

import com.sushishop.ai.OrderDraftStatus;
import com.sushishop.shared.address.AddressResponse;
import com.sushishop.shared.enums.DeliveryMethod;
import com.sushishop.shared.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AiOrderDraftResponse(
        UUID id,
        OrderDraftStatus status,
        String customerName,
        String phone,
        PaymentMethod paymentMethod,
        DeliveryMethod deliveryMethod,
        AddressResponse address,
        List<AiOrderDraftItemResponse> items,
        BigDecimal totalAmount,
        Long confirmedOrderId
) {
}
