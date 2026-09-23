package com.sushishop.ai;

import com.sushishop.ai.dto.DraftOrderInput;
import com.sushishop.order.OrderCreationService;
import com.sushishop.order.dto.request.OrderItemRequest;
import com.sushishop.order.dto.response.OrderResponse;
import com.sushishop.product.Product;
import com.sushishop.product.ProductRepository;
import com.sushishop.shared.enums.Category;
import com.sushishop.shared.enums.DeliveryMethod;
import com.sushishop.shared.enums.OrderStatus;
import com.sushishop.shared.enums.PaymentMethod;
import com.sushishop.shared.exception.core.ConflictException;
import com.sushishop.user.User;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiOrderDraftServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;
    @Mock
    private OrderDraftRepository draftRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderCreationService orderCreationService;
    @Mock
    private Validator validator;

    private AiOrderDraftService service;
    private UUID sessionId;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        service = new AiOrderDraftService(
                sessionRepository, draftRepository, productRepository, orderCreationService, validator);
        sessionId = UUID.randomUUID();
        var user = User.builder()
                .id(7L)
                .name("Minh")
                .email("minh@example.com")
                .phone("+380961791111")
                .build();
        session = ChatSession.builder().id(sessionId).user(user).build();
    }

    @Test
    void createsDraftFromCurrentProductPriceAndUserProfile() {
        var product = product(1L, "Salmon Roll", "250.00", true);
        var input = new DraftOrderInput(
                null, null, null, DeliveryMethod.PICKUP, null,
                List.of(new OrderItemRequest(1L, 2)));

        when(sessionRepository.findByIdAndUserEmail(sessionId, "minh@example.com"))
                .thenReturn(Optional.of(session));
        when(validator.validate(any())).thenReturn(Set.of());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(draftRepository.findOwnedForUpdate(sessionId, "minh@example.com"))
                .thenReturn(Optional.empty());
        when(draftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createOrReplace(sessionId, "minh@example.com", input);

        assertThat(result.customerName()).isEqualTo("Minh");
        assertThat(result.phone()).isEqualTo("+380961791111");
        assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.ON_DELIVERY);
        assertThat(result.totalAmount()).isEqualByComparingTo("500.00");
        assertThat(result.items()).singleElement()
                .satisfies(item -> assertThat(item.unitPrice()).isEqualByComparingTo("250.00"));
    }

    @Test
    void rejectsConfirmationWhenPriceChangedAfterDraftWasCreated() {
        var draft = draft(OrderDraftStatus.PENDING, "250.00");
        var changedProduct = product(1L, "Salmon Roll", "300.00", true);
        when(draftRepository.findOwnedForUpdate(sessionId, "minh@example.com"))
                .thenReturn(Optional.of(draft));
        when(productRepository.findById(1L)).thenReturn(Optional.of(changedProduct));

        assertThatThrownBy(() -> service.confirm(sessionId, "minh@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("prices changed");

        verify(orderCreationService, never()).create(any(), any());
    }

    @Test
    void confirmsPendingDraftOnlyOnce() {
        var draft = draft(OrderDraftStatus.PENDING, "250.00");
        var product = product(1L, "Salmon Roll", "250.00", true);
        var response = new OrderResponse(
                91L, "Minh", "minh@example.com", "+380961791111", null,
                DeliveryMethod.PICKUP, PaymentMethod.ON_DELIVERY, "ON_DELIVERY",
                OrderStatus.CONFIRMED, new BigDecimal("250.00"), null, List.of());

        when(draftRepository.findOwnedForUpdate(sessionId, "minh@example.com"))
                .thenReturn(Optional.of(draft));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderCreationService.create(any(), any())).thenReturn(response);

        var result = service.confirm(sessionId, "minh@example.com");

        assertThat(result.id()).isEqualTo(91L);
        assertThat(draft.getStatus()).isEqualTo(OrderDraftStatus.CONFIRMED);
        assertThat(draft.getConfirmedOrderId()).isEqualTo(91L);

        assertThatThrownBy(() -> service.confirm(sessionId, "minh@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already confirmed");
        verify(orderCreationService).create(any(), any());
    }

    private Product product(Long id, String name, String price, boolean available) {
        return Product.builder()
                .id(id)
                .name(name)
                .slug("salmon-roll")
                .price(new BigDecimal(price))
                .category(Category.ROLL)
                .weight(250)
                .available(available)
                .build();
    }

    private OrderDraft draft(OrderDraftStatus status, String unitPrice) {
        var price = new BigDecimal(unitPrice);
        return OrderDraft.builder()
                .id(UUID.randomUUID())
                .session(session)
                .status(status)
                .customerName("Minh")
                .phone("+380961791111")
                .paymentMethod(PaymentMethod.ON_DELIVERY)
                .deliveryMethod(DeliveryMethod.PICKUP)
                .items(new ArrayList<>(List.of(OrderDraftItem.builder()
                        .productId(1L)
                        .productName("Salmon Roll")
                        .quantity(1)
                        .unitPrice(price)
                        .subtotal(price)
                        .build())))
                .totalAmount(price)
                .build();
    }
}
