package com.sushishop.ai;

import com.sushishop.ai.dto.AiOrderDraftResponse;
import com.sushishop.ai.dto.DraftOrderInput;
import com.sushishop.audit.Auditable;
import com.sushishop.order.OrderQueryService;
import com.sushishop.product.ProductQueryService;
import com.sushishop.product.ProductService;
import com.sushishop.product.dto.response.ProductListResponse;
import com.sushishop.promotion.PromotionService;
import com.sushishop.shared.enums.AuditAction;
import com.sushishop.shared.enums.Category;
import com.sushishop.shared.exception.core.SushiShopException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("ai")
@RequiredArgsConstructor
public class AiTools {

    public static final String USER_EMAIL = "userEmail";
    public static final String SESSION_ID = "sessionId";

    private final ProductQueryService productQueryService;
    private final ProductService productService;
    private final PromotionService promotionService;
    private final OrderQueryService orderQueryService;
    private final AiOrderDraftService draftService;

    @Tool(description = "Search the current available sushi catalog by name or description. "
            + "Use this before recommending products. Category is optional.")
    @Auditable(action = AuditAction.AI_TOOL_CALL, entity = "AiTool:searchProducts")
    public String searchProducts(
            @ToolParam(description = "Words from the product name or description", required = false)
            String query,
            @ToolParam(description = "Optional category: ROLL, SET, DRINK, DESSERT, SOUP, SALAD, WOK or EXTRA",
                    required = false)
            String category
    ) {
        return safely("searchProducts", () -> {
            Category parsedCategory = null;
            if (category != null && !category.isBlank()) {
                parsedCategory = Category.valueOf(category.trim().toUpperCase());
            }

            var page = productQueryService.getAll(
                    PageRequest.of(0, 8, Sort.by("name").ascending()), query, parsedCategory, true);
            if (page.isEmpty()) {
                return "No available products matched that search.";
            }

            return page.getContent().stream()
                    .map(product -> "id=%d, name=%s, category=%s, basePrice=%s UAH, available=%s"
                            .formatted(product.id(), product.name(), product.category(),
                                    product.price().toPlainString(), product.available()))
                    .collect(Collectors.joining("\n"));
        });
    }

    @Tool(description = "Get live details for one product by its numeric product ID.")
    @Auditable(action = AuditAction.AI_TOOL_CALL, entity = "AiTool:getProductDetails")
    public String getProductDetails(
            @ToolParam(description = "Numeric product ID returned by searchProducts") Long productId
    ) {
        return safely("getProductDetails", () -> {
            var product = productService.getById(productId);
            if (!product.available()) {
                return "That product currently exists but is not available to order.";
            }
            return """
                    id=%d
                    name=%s
                    description=%s
                    category=%s
                    basePrice=%s UAH
                    weight=%s g
                    pieces=%s
                    available=true
                    checkout uses the current base price; do not automatically apply an advertised discount
                    """.formatted(
                    product.id(), product.name(), valueOrUnknown(product.description()), product.category(),
                    product.price().toPlainString(), product.weight(), valueOrUnknown(product.pieces()));
        });
    }

    @Tool(description = "List promotions that are active at the current time. "
            + "Promotions are informational; checkout currently uses product base prices.")
    @Auditable(action = AuditAction.AI_TOOL_CALL, entity = "AiTool:getActivePromotions")
    public String getActivePromotions() {
        return safely("getActivePromotions", () -> {
            var promotions = promotionService.getActive();
            if (promotions.isEmpty()) {
                return "There are no active promotions right now.";
            }
            return promotions.stream()
                    .map(promotion -> "%s: %s%% advertised discount, valid until %s, applies to: %s"
                            .formatted(promotion.title(), promotion.discountPercent().toPlainString(),
                                    promotion.endDate(),
                                    valueOrUnknown(promotion.products().stream()
                                            .map(ProductListResponse::name)
                                            .collect(Collectors.joining(", ")))))
                    .collect(Collectors.joining("\n"));
        });
    }
I
    @Tool(description = "Get recent order statuses for the currently authenticated customer. "
            + "Never ask for or accept an email address for this tool.")
    @Auditable(action = AuditAction.AI_TOOL_CALL, entity = "AiTool:getMyOrderStatus")
    public String getMyOrderStatus(ToolContext toolContext) {
        return safely("getMyOrderStatus", () -> {
            String email = contextEmail(toolContext);
            var orders = orderQueryService.getByUser(
                    email, PageRequest.of(0, 10, Sort.by("createdAt").descending()));
            if (orders.isEmpty()) {
                return "The authenticated customer has no orders.";
            }
            return orders.getContent().stream()
                    .map(order -> "orderId=%d, status=%s, paymentStatus=%s, total=%s UAH, createdAt=%s"
                            .formatted(order.id(), order.status(), order.paymentStatus(),
                                    order.totalAmount().toPlainString(), order.createdAt()))
                    .collect(Collectors.joining("\n"));
        });
    }

    @Tool(description = "Create or replace a priced order DRAFT for the authenticated customer. "
            + "This never creates or pays a real order. Search products first and use their numeric IDs. "
            + "For DELIVERY include an address; for PICKUP address may be null. "
            + "Customer name, phone and payment method may be omitted to use profile/default values.")
    @Auditable(action = AuditAction.AI_TOOL_CALL, entity = "AiTool:draftOrder")
    public String draftOrder(
            @ToolParam(description = "Checkout details and a non-empty list of productId/quantity items")
            DraftOrderInput input,
            ToolContext toolContext
    ) {
        return safely("draftOrder", () -> {
            String email = contextEmail(toolContext);
            UUID sessionId = contextSessionId(toolContext);
            var draft = draftService.createOrReplace(sessionId, email, input);
            return formatDraft(draft) + "\nThis is only a draft. The customer must explicitly confirm it.";
        });
    }

    private String formatDraft(AiOrderDraftResponse draft) {
        String items = draft.items().stream()
                .map(item -> "%s x%d = %s UAH"
                        .formatted(item.productName(), item.quantity(), item.subtotal().toPlainString()))
                .collect(Collectors.joining(", "));
        return "Draft saved: %s. Total: %s UAH. Delivery: %s. Payment: %s."
                .formatted(items, draft.totalAmount().toPlainString(),
                        draft.deliveryMethod(), draft.paymentMethod());
    }

    private String safely(String toolName, Supplier<String> action) {
        try {
            return action.get();
        } catch (SushiShopException | IllegalArgumentException ex) {
            return "Tool could not complete the request: " + ex.getMessage();
        } catch (RuntimeException ex) {
            log.error("AI tool {} failed", toolName, ex);
            return "Tool is temporarily unavailable.";
        }
    }

    private String contextEmail(ToolContext context) {
        Object value = context.getContext().get(USER_EMAIL);
        if (!(value instanceof String email) || email.isBlank()) {
            throw new IllegalArgumentException("Authenticated user context is missing");
        }
        return email;
    }

    private UUID contextSessionId(ToolContext context) {
        Object value = context.getContext().get(SESSION_ID);
        if (value instanceof UUID id) {
            return id;
        }
        throw new IllegalArgumentException("Chat session context is missing");
    }

    private String valueOrUnknown(Object value) {
        return value == null || value.toString().isBlank() ? "not provided" : value.toString();
    }
}
