package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.NotificationRestClient;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationRestClient notificationClient;

    @InjectMocks
    private OrderService orderService;

    private MockedStatic<TenantContext> mockedTenantContext;

    private static final String TENANT_ID = "tenant-abc-123";
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("99.99");

    @BeforeEach
    void setUp() {
        mockedTenantContext = mockStatic(TenantContext.class);
        mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        mockedTenantContext.close();
    }

    private Order buildOrder(UUID id, OrderStatus status) {
        return Order.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .customerEmail(CUSTOMER_EMAIL)
                .totalAmount(TOTAL_AMOUNT)
                .status(status)
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("Should persist pending order and send notification")
        void shouldCreateOrderSuccessfully() {
            OrderCreateRequest request = new OrderCreateRequest();
            request.setCustomerEmail(CUSTOMER_EMAIL);
            request.setTotalAmount(TOTAL_AMOUNT);

            Order savedOrder = buildOrder(ORDER_ID, OrderStatus.PENDING);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            OrderResponse response = orderService.createOrder(request);

            // Assert repository input
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order captured = orderCaptor.getValue();
            assertThat(captured.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(captured.getCustomerEmail()).isEqualTo(CUSTOMER_EMAIL);
            assertThat(captured.getTotalAmount()).isEqualByComparingTo(TOTAL_AMOUNT);
            assertThat(captured.getStatus()).isEqualTo(OrderStatus.PENDING);

            // Assert notification call
            verify(notificationClient).sendNotification(ORDER_ID, "ORDER_CREATED", CUSTOMER_EMAIL, TOTAL_AMOUNT);

            // Assert response mapping
            assertThat(response.getId()).isEqualTo(ORDER_ID);
            assertThat(response.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getCustomerEmail()).isEqualTo(CUSTOMER_EMAIL);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(TOTAL_AMOUNT);
        }
    }

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should send ORDER_COMPLETED event when new status is COMPLETED")
        void shouldUpdateOrderStatusToCompleted() {
            Order existingOrder = buildOrder(ORDER_ID, OrderStatus.PENDING);
            Order updatedOrder = buildOrder(ORDER_ID, OrderStatus.COMPLETED);

            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);

            OrderResponse response = orderService.updateOrderStatus(ORDER_ID, OrderStatus.COMPLETED);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            verify(notificationClient).sendNotification(ORDER_ID, "ORDER_COMPLETED", CUSTOMER_EMAIL, TOTAL_AMOUNT);
        }

        @Test
        @DisplayName("Should send ORDER_UPDATED event when new status is not COMPLETED")
        void shouldUpdateOrderStatusToOtherStatus() {
            Order existingOrder = buildOrder(ORDER_ID, OrderStatus.PENDING);
            Order updatedOrder = buildOrder(ORDER_ID, OrderStatus.PROCESSING);

            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);

            OrderResponse response = orderService.updateOrderStatus(ORDER_ID, OrderStatus.PROCESSING);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSING);
            verify(notificationClient).sendNotification(ORDER_ID, "ORDER_UPDATED", CUSTOMER_EMAIL, TOTAL_AMOUNT);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when order does not exist")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateOrderStatus(ORDER_ID, OrderStatus.COMPLETED))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ORDER_ID.toString());

            verify(orderRepository, never()).save(any());
            verify(notificationClient, never()).sendNotification(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel pending order successfully")
        void shouldCancelOrderSuccessfully() {
            Order existingOrder = buildOrder(ORDER_ID, OrderStatus.PENDING);
            Order cancelledOrder = buildOrder(ORDER_ID, OrderStatus.CANCELLED);

            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(existingOrder)).thenReturn(cancelledOrder);

            OrderResponse response = orderService.cancelOrder(ORDER_ID);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(notificationClient).sendNotification(ORDER_ID, "ORDER_CANCELLED", CUSTOMER_EMAIL, TOTAL_AMOUNT);
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"COMPLETED", "CANCELLED"})
        @DisplayName("Should throw IllegalStateException if order is already completed or cancelled")
        void shouldThrowExceptionWhenCancellingTerminalOrder(OrderStatus terminalStatus) {
            Order order = buildOrder(ORDER_ID, terminalStatus);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot cancel an order that is already completed or cancelled.");

            verify(orderRepository, never()).save(any());
            verify(notificationClient, never()).sendNotification(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when order to cancel is not found")
        void shouldThrowExceptionWhenCancelOrderNotFound() {
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ORDER_ID.toString());
        }
    }

    @Nested
    @DisplayName("getOrdersForCurrentTenant")
    class GetOrdersTests {

        @Test
        @DisplayName("Should return mapped list of orders for active tenant")
        void shouldReturnTenantOrders() {
            Order order1 = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
            Order order2 = buildOrder(UUID.randomUUID(), OrderStatus.COMPLETED);

            when(orderRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(order1, order2));

            List<OrderResponse> result = orderService.getOrdersForCurrentTenant();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(order1.getId());
            assertThat(result.get(1).getId()).isEqualTo(order2.getId());
        }

        @Test
        @DisplayName("Should return empty list when tenant has no orders")

void shouldReturnEmptyListWhenNoOrders() {
when(orderRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());
List result = orderService.getOrdersForCurrentTenant();
assertThat(result).isEmpty();
}
}
@Nested
@DisplayName("getOrderById")
class GetOrderByIdTests {
@Test
@DisplayName("Should return order when found for tenant")
void shouldReturnOrderWhenFound() {
Order order = buildOrder(ORDER_ID, OrderStatus.PROCESSING);
when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
OrderResponse response = orderService.getOrderById(ORDER_ID);
assertThat(response.getId()).isEqualTo(ORDER_ID);
assertThat(response.getTenantId()).isEqualTo(TENANT_ID);
assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSING);
}
@Test
@DisplayName("Should throw EntityNotFoundException when order id does not match tenant")
void shouldThrowExceptionWhenOrderNotFound() {
when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.empty());
assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID))
.isInstanceOf(EntityNotFoundException.class)
.hasMessageContaining(ORDER_ID.toString());
}
}
}