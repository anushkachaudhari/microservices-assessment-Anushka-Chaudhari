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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String TENANT = "tenant-acme";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Order orderWithStatus(UUID id, OrderStatus status) {
        return Order.builder()
                .id(id)
                .tenantId(TENANT)
                .customerEmail("buyer@example.com")
                .totalAmount(new BigDecimal("25.00"))
                .status(status)
                .build();
    }

    @Test
    @DisplayName("createOrder persists a PENDING order under the current tenant and publishes ORDER_CREATED")
    void createOrder_publishesCreatedEvent() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail("buyer@example.com");
        request.setTotalAmount(new BigDecimal("25.00"));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTenantId()).isEqualTo(TENANT);

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ORDER_CREATED");
        assertThat(captor.getValue().tenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("updateOrderStatus allows a legal transition and emits ORDER_COMPLETED for COMPLETED")
    void updateOrderStatus_legalTransition_completed() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndTenantId(id, TENANT))
                .thenReturn(Optional.of(orderWithStatus(id, OrderStatus.PROCESSING)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(id, OrderStatus.COMPLETED);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ORDER_COMPLETED");
    }

    @Test
    @DisplayName("updateOrderStatus rejects an illegal transition (COMPLETED is terminal) with 409 semantics")
    void updateOrderStatus_illegalTransition_throws() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndTenantId(id, TENANT))
                .thenReturn(Optional.of(orderWithStatus(id, OrderStatus.COMPLETED)));

        assertThatThrownBy(() -> orderService.updateOrderStatus(id, OrderStatus.PROCESSING))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("updateOrderStatus on a missing order throws OrderNotFoundException")
    void updateOrderStatus_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(id, OrderStatus.PROCESSING))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("cancelOrder cancels a PENDING order and publishes ORDER_CANCELLED")
    void cancelOrder_pending_success() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndTenantId(id, TENANT))
                .thenReturn(Optional.of(orderWithStatus(id, OrderStatus.PENDING)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(id);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    @DisplayName("cancelOrder on an already-COMPLETED order is rejected and emits no event")
    void cancelOrder_completed_throws() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndTenantId(id, TENANT))
                .thenReturn(Optional.of(orderWithStatus(id, OrderStatus.COMPLETED)));

        assertThatThrownBy(() -> orderService.cancelOrder(id))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
