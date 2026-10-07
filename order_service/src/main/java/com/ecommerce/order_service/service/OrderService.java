package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.event.OrderEvent;
import com.ecommerce.order_service.exception.InvalidOrderStateException;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        String tenantId = TenantContext.getTenantId();

        Order order = Order.builder()
                .tenantId(tenantId)
                .customerEmail(request.getCustomerEmail())
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        publishEvent(savedOrder, "ORDER_CREATED");

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        String tenantId = TenantContext.getTenantId();

        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidOrderStateException(
                    "Cannot transition order %s from %s to %s".formatted(orderId, order.getStatus(), newStatus));
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        publishEvent(updatedOrder, eventTypeFor(newStatus));

        return mapToResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        String tenantId = TenantContext.getTenantId();

        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order %s in status %s".formatted(orderId, order.getStatus()));
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        publishEvent(cancelledOrder, "ORDER_CANCELLED");

        return mapToResponse(cancelledOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForCurrentTenant() {
        String tenantId = TenantContext.getTenantId();
        return orderRepository.findByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        String tenantId = TenantContext.getTenantId();
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return mapToResponse(order);
    }

    private void publishEvent(Order order, String eventType) {
        eventPublisher.publishEvent(new OrderEvent(
                order.getId(),
                order.getTenantId(),
                eventType,
                order.getCustomerEmail(),
                order.getTotalAmount()));
    }

    private String eventTypeFor(OrderStatus status) {
        return switch (status) {
            case COMPLETED -> "ORDER_COMPLETED";
            case CANCELLED -> "ORDER_CANCELLED";
            default -> "ORDER_UPDATED";
        };
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setTenantId(order.getTenantId());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }
}
