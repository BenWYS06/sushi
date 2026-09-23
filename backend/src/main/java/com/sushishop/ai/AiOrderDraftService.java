package com.sushishop.ai;

import com.sushishop.ai.dto.AiOrderDraftItemResponse;
import com.sushishop.ai.dto.AiOrderDraftResponse;
import com.sushishop.ai.dto.DraftOrderInput;
import com.sushishop.order.OrderCreationService;
import com.sushishop.order.dto.request.CreateOrderRequest;
import com.sushishop.order.dto.request.OrderItemRequest;
import com.sushishop.order.dto.response.OrderResponse;
import com.sushishop.product.ProductRepository;
import com.sushishop.shared.address.AddressRequest;
import com.sushishop.shared.address.AddressResponse;
import com.sushishop.shared.enums.DeliveryMethod;
import com.sushishop.shared.enums.PaymentMethod;
import com.sushishop.shared.exception.core.BadRequestException;
import com.sushishop.shared.exception.core.ConflictException;
import com.sushishop.shared.exception.core.NotFoundException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("ai")
@RequiredArgsConstructor
public class AiOrderDraftService {

    private final ChatSessionRepository sessionRepository;
    private final OrderDraftRepository draftRepository;
    private final ProductRepository productRepository;
    private final OrderCreationService orderCreationService;
    private final Validator validator;

    @Transactional
    public AiOrderDraftResponse createOrReplace(UUID sessionId, String email, DraftOrderInput input) {
        var session = sessionRepository.findByIdAndUserEmail(sessionId, email)
                .orElseThrow(() -> new NotFoundException("AI chat session not found"));

        var request = resolveRequest(input, session);
        validateRequest(request);
        ensureProductsAreUnique(request.items());

        var draftItems = request.items().stream()
                .map(item -> {
                    var product = productRepository.findById(item.productId())
                            .orElseThrow(() -> new NotFoundException("Product not found: " + item.productId()));
                    if (!product.isAvailable()) {
                        throw new BadRequestException("Product is not available: " + product.getName());
                    }

                    var subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
                    return OrderDraftItem.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .quantity(item.quantity())
                            .unitPrice(product.getPrice())
                            .subtotal(subtotal)
                            .build();
                })
                .toList();

        // Lock an existing draft so replacing and confirming cannot modify it at the same time.
        var draft = draftRepository.findOwnedForUpdate(sessionId, email).orElseGet(OrderDraft::new);
        draft.setSession(session);
        draft.setStatus(OrderDraftStatus.PENDING);
        draft.setConfirmedOrderId(null);
        copyCheckoutDetails(draft, request);
        draft.getItems().clear();
        draft.getItems().addAll(draftItems);
        draft.setTotalAmount(calculateTotal(draftItems));

        return toResponse(draftRepository.save(draft));
    }

    @Transactional(readOnly = true)
    public Optional<AiOrderDraftResponse> findBySession(UUID sessionId) {
        return draftRepository.findBySessionId(sessionId).map(this::toResponse);
    }

    @Transactional
    public OrderResponse confirm(UUID sessionId, String email) {
        // The database lock makes repeated confirm clicks wait for the same draft.
        var draft = draftRepository.findOwnedForUpdate(sessionId, email)
                .orElseThrow(() -> new NotFoundException("Order draft not found"));

        if (draft.getStatus() != OrderDraftStatus.PENDING) {
            throw new ConflictException("This order draft was already confirmed");
        }

        var items = draft.getItems().stream().map(item -> {
            var product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + item.getProductId()));
            if (!product.isAvailable()) {
                throw new ConflictException("Product is no longer available: " + product.getName());
            }
            if (product.getPrice().compareTo(item.getUnitPrice()) != 0) {
                throw new ConflictException("Product prices changed. Please create a new order draft");
            }
            return new OrderItemRequest(item.getProductId(), item.getQuantity());
        }).toList();

        var request = new CreateOrderRequest(
                draft.getCustomerName(),
                draft.getPhone(),
                draft.getPaymentMethod(),
                draft.getDeliveryMethod(),
                toAddressRequest(draft),
                items
        );

        var order = orderCreationService.create(request, email);
        draft.setStatus(OrderDraftStatus.CONFIRMED);
        draft.setConfirmedOrderId(order.id());
        draftRepository.save(draft);
        return order;
    }

    private CreateOrderRequest resolveRequest(DraftOrderInput input, ChatSession session) {
        if (input == null) {
            throw new BadRequestException("Order draft information is required");
        }

        var user = session.getUser();
        String customerName = firstNotBlank(input.customerName(), user.getName());
        String phone = firstNotBlank(input.phone(), user.getPhone());
        PaymentMethod paymentMethod = input.paymentMethod() != null
                ? input.paymentMethod() : PaymentMethod.ON_DELIVERY;
        DeliveryMethod deliveryMethod = input.deliveryMethod();
        AddressRequest address = input.address() == null ? null : new AddressRequest(
                input.address().city(), input.address().street(), input.address().house(),
                input.address().apartment(), input.address().comment());

        if (deliveryMethod == DeliveryMethod.DELIVERY && address == null
                && hasText(user.getCity()) && hasText(user.getStreet()) && hasText(user.getHouse())) {
            address = new AddressRequest(
                    user.getCity(), user.getStreet(), user.getHouse(), user.getApartment(), null);
        }

        return new CreateOrderRequest(
                customerName, phone, paymentMethod, deliveryMethod, address, input.items());
    }

    private void validateRequest(CreateOrderRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new BadRequestException(violations.iterator().next().getMessage());
        }
        if (request.deliveryMethod() == DeliveryMethod.DELIVERY && request.address() == null) {
            throw new BadRequestException("Address is required for delivery");
        }
    }

    private void ensureProductsAreUnique(List<OrderItemRequest> items) {
        var productIds = new HashSet<Long>();
        if (items.stream().anyMatch(item -> !productIds.add(item.productId()))) {
            throw new BadRequestException("Each product may appear only once in an order draft");
        }
    }

    private void copyCheckoutDetails(OrderDraft draft, CreateOrderRequest request) {
        draft.setCustomerName(request.customerName());
        draft.setPhone(request.phone());
        draft.setPaymentMethod(request.paymentMethod());
        draft.setDeliveryMethod(request.deliveryMethod());

        var address = request.address();
        draft.setCity(address != null ? address.city() : null);
        draft.setStreet(address != null ? address.street() : null);
        draft.setHouse(address != null ? address.house() : null);
        draft.setApartment(address != null ? address.apartment() : null);
        draft.setAddressComment(address != null ? address.comment() : null);
    }

    private BigDecimal calculateTotal(List<OrderDraftItem> items) {
        return items.stream()
                .map(OrderDraftItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AiOrderDraftResponse toResponse(OrderDraft draft) {
        var items = draft.getItems().stream()
                .map(item -> new AiOrderDraftItemResponse(
                        item.getProductId(), item.getProductName(), item.getQuantity(),
                        item.getUnitPrice(), item.getSubtotal()))
                .toList();

        AddressResponse address = draft.getDeliveryMethod() == DeliveryMethod.DELIVERY
                ? new AddressResponse(draft.getCity(), draft.getStreet(), draft.getHouse(),
                draft.getApartment(), draft.getAddressComment())
                : null;

        return new AiOrderDraftResponse(
                draft.getId(), draft.getStatus(), draft.getCustomerName(), draft.getPhone(),
                draft.getPaymentMethod(), draft.getDeliveryMethod(), address, items,
                draft.getTotalAmount(), draft.getConfirmedOrderId());
    }

    private AddressRequest toAddressRequest(OrderDraft draft) {
        if (draft.getDeliveryMethod() == DeliveryMethod.PICKUP) {
            return null;
        }
        return new AddressRequest(
                draft.getCity(), draft.getStreet(), draft.getHouse(),
                draft.getApartment(), draft.getAddressComment());
    }

    private String firstNotBlank(String first, String fallback) {
        return hasText(first) ? first : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
