package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.NotificationRestClient;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final NotificationRestClient notificationClient;

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

        // Trigger notification asynchronously or synchronously via client
        notificationClient.sendNotification(savedOrder.getId(), "ORDER_CREATED", savedOrder.getCustomerEmail(),
                savedOrder.getTotalAmount());

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        String tenantId = TenantContext.getTenantId();

        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        String eventType = (newStatus == OrderStatus.COMPLETED) ? "ORDER_COMPLETED" : "ORDER_UPDATED";
        notificationClient.sendNotification(updatedOrder.getId(), eventType, updatedOrder.getCustomerEmail(),
                updatedOrder.getTotalAmount());

        return mapToResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        String tenantId = TenantContext.getTenantId();

        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel an order that is already completed or cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        notificationClient.sendNotification(cancelledOrder.getId(), "ORDER_CANCELLED",
                cancelledOrder.getCustomerEmail(), cancelledOrder.getTotalAmount());

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
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));
        return mapToResponse(order);
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