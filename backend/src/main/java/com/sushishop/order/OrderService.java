package com.sushishop.order;

import com.sushishop.audit.Auditable;
import com.sushishop.order.dto.response.OrderResponse;
import com.sushishop.order.dto.response.OrderStatusUpdateResponse;
import com.sushishop.payment.PaymentConfirmedEvent;
import com.sushishop.shared.enums.AuditAction;
import com.sushishop.shared.enums.OrderStatus;
import com.sushishop.shared.exception.core.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sushishop.shared.enums.DeliveryMethod;
import com.sushishop.shared.enums.UserRole;
import com.sushishop.shared.exception.core.BadRequestException;
import com.sushishop.user.UserRepository;
import com.sushishop.shared.exception.core.ForbiddenException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
private final OrderMapper orderMapper;
private final SimpMessagingTemplate messagingTemplate;
private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        log.info("Getting order by id: {}", id);
        return orderRepository.findById(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    @Auditable(action = AuditAction.UPDATE, entity = "Order")
@Transactional
public OrderResponse updateStatus(
        Long id,
        OrderStatus newStatus,
        String currentUserEmail,
        boolean isCourier
) {
    var order = orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found: " + id));

    if (isCourier) {
        validateCourierStatusUpdate(order, newStatus, currentUserEmail);
    } else {
        validateAdminStatusUpdate(order, newStatus);
    }

    order.getStatus().validateTransition(
            newStatus,
            order.getDeliveryMethod()
    );

    order.setStatus(newStatus);
    var updated = orderRepository.save(order);

    broadcastOrderStatus(updated);

    log.info(
            "Order {} status updated to {} by {}",
            id,
            newStatus,
            currentUserEmail
    );

    return orderMapper.toResponse(updated);
}

    @EventListener
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        confirmOrder(event.getPayment().getOrder().getId());
    }

    @Transactional
public void confirmOrder(Long orderId) {
    var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

    order.setStatus(OrderStatus.CONFIRMED);
    var updated = orderRepository.save(order);

    broadcastOrderStatus(updated);
    log.info("Order {} confirmed after payment", orderId);
}

    @Auditable(action = AuditAction.UPDATE, entity = "Order")
@Transactional
public OrderResponse dispatch(Long orderId, Long courierId) {
    var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

    if (order.getDeliveryMethod() != DeliveryMethod.DELIVERY) {
        throw new BadRequestException("Only delivery orders can be dispatched");
    }

    order.getStatus().validateTransition(
            OrderStatus.DELIVERING,
            order.getDeliveryMethod()
    );

    var courier = userRepository.findById(courierId)
            .orElseThrow(() -> new NotFoundException("Courier not found: " + courierId));

    if (courier.getUserRole() != UserRole.COURIER) {
        throw new BadRequestException("Selected user is not a courier");
    }

    order.setCourier(courier);
    order.setStatus(OrderStatus.DELIVERING);

    var updated = orderRepository.save(order);

   broadcastOrderStatus(updated);

    log.info(
            "Order {} dispatched to courier {}",
            updated.getId(),
            courier.getEmail()
    );

    return orderMapper.toResponse(updated);
}

private void validateAdminStatusUpdate(Order order, OrderStatus newStatus) {
    if (newStatus == OrderStatus.DELIVERING) {
        throw new BadRequestException(
                "Use the dispatch endpoint to assign a courier and start delivery"
        );
    }

    if (newStatus == OrderStatus.DELIVERED
            && order.getDeliveryMethod() == DeliveryMethod.DELIVERY) {
        throw new ForbiddenException(
                "Only the assigned courier can complete a delivery order"
        );
    }
}

private void validateCourierStatusUpdate(
        Order order,
        OrderStatus newStatus,
        String courierEmail
) {
    if (newStatus != OrderStatus.DELIVERED) {
        throw new ForbiddenException(
                "Courier can only change an order to DELIVERED"
        );
    }

    if (order.getDeliveryMethod() != DeliveryMethod.DELIVERY) {
        throw new ForbiddenException(
                "Courier cannot complete a pickup order"
        );
    }

    if (order.getCourier() == null
            || !order.getCourier().getEmail().equals(courierEmail)) {
        throw new ForbiddenException(
                "Order is not assigned to this courier"
        );
    }
}

private void broadcastOrderStatus(Order order) {
    var update = new OrderStatusUpdateResponse(
            order.getId(),
            order.getStatus().name()
    );

    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
        sendOrderStatus(update);
        return;
    }

// Prevent send message before transaction commit
    TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendOrderStatus(update);
                }
            }
    );
}

private void sendOrderStatus(OrderStatusUpdateResponse update) {
    messagingTemplate.convertAndSend(
            "/topic/orders/" + update.orderId(),
            update
    );
    messagingTemplate.convertAndSend("/topic/orders/changed", update);
}
}
