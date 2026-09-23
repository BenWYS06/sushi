package com.sushishop.ai;

import com.sushishop.order.OrderQueryService;
import com.sushishop.product.ProductQueryService;
import com.sushishop.product.ProductService;
import com.sushishop.promotion.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiToolsSchemaTest {

    @Test
    void exposesOnlyTheFiveExpectedAgentTools() {
        var tools = new AiTools(
                mock(ProductQueryService.class),
                mock(ProductService.class),
                mock(PromotionService.class),
                mock(OrderQueryService.class),
                mock(AiOrderDraftService.class));

        var callbacks = ToolCallbacks.from(tools);
        var definitions = Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition())
                .toList();

        assertThat(definitions)
                .extracting(definition -> definition.name())
                .containsExactlyInAnyOrder(
                        "searchProducts",
                        "getProductDetails",
                        "getActivePromotions",
                        "getMyOrderStatus",
                        "draftOrder");

        var privateOrderToolSchemas = definitions.stream()
                .filter(definition -> definition.name().equals("getMyOrderStatus")
                        || definition.name().equals("draftOrder"))
                .map(definition -> definition.inputSchema())
                .toList();

        assertThat(privateOrderToolSchemas).noneMatch(schema -> schema.contains("userEmail"));
        assertThat(privateOrderToolSchemas).noneMatch(schema -> schema.contains("sessionId"));
    }
}
