        package com.sushishop.ai.dto;

import com.sushishop.order.dto.request.OrderItemRequest;
import com.sushishop.shared.enums.DeliveryMethod;
import com.sushishop.shared.enums.PaymentMethod;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/** Arguments the model may supply to the draftOrder tool. */
public record DraftOrderInput(
        @ToolParam(description = "Recipient name; omit to use the authenticated user's name", required = false)
        String customerName,

        @ToolParam(description = "Contact phone; omit to use the authenticated user's phone", required = false)
        String phone,

        @ToolParam(description = "ONLINE or ON_DELIVERY; omit to use ON_DELIVERY", required = false)
        PaymentMethod paymentMethod,

        @ToolParam(description = "DELIVERY or PICKUP")
        DeliveryMethod deliveryMethod,

        @ToolParam(description = "Delivery address; omit for PICKUP or to use a complete saved profile address",
                required = false)
        DraftAddressInput address,

        @ToolParam(description = "Products and positive quantities to put in the draft")
        List<OrderItemRequest> items
) {
}
