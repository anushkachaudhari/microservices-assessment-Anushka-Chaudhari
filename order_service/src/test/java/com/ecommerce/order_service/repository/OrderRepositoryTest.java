package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";

    private Order createOrder(String tenantId, String email, BigDecimal amount, OrderStatus status) {
        return Order.builder()
                .tenantId(tenantId)
                .customerEmail(email)
                .totalAmount(amount)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("findByTenantId - Should return only orders matching the given tenant ID")
    void findByTenantId_ReturnsOnlyTenantOrders() {
        entityManager.persist(createOrder(TENANT_A, "user1@example.com", new BigDecimal("10.00"), OrderStatus.PENDING));
        entityManager
                .persist(createOrder(TENANT_A, "user2@example.com", new BigDecimal("20.00"), OrderStatus.PROCESSING));
        entityManager.persist(createOrder(TENANT_B, "user3@example.com", new BigDecimal("30.00"), OrderStatus.PENDING));
        entityManager.flush();

        List<Order> tenantAOrders = orderRepository.findByTenantId(TENANT_A);

        assertEquals(2, tenantAOrders.size());
        assertTrue(tenantAOrders.stream().allMatch(o -> TENANT_A.equals(o.getTenantId())));
    }

    @Test
    @DisplayName("findByTenantId - Should return empty list when tenant has no orders")
    void findByTenantId_ReturnsEmptyListWhenNoMatches() {
        entityManager.persist(createOrder(TENANT_A, "user1@example.com", new BigDecimal("10.00"), OrderStatus.PENDING));
        entityManager.flush();

        List<Order> orders = orderRepository.findByTenantId("non-existent-tenant");

        assertTrue(orders.isEmpty());
    }

    @Test
    @DisplayName("findByIdAndTenantId - Should return order when ID and tenant match")
    void findByIdAndTenantId_ReturnsOrderWhenBothMatch() {
        Order saved = entityManager.persistFlushFind(
                createOrder(TENANT_A, "buyer@example.com", new BigDecimal("99.99"), OrderStatus.PENDING));

        Optional<Order> result = orderRepository.findByIdAndTenantId(saved.getId(), TENANT_A);

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
        assertEquals(TENANT_A, result.get().getTenantId());
    }

    @Test
    @DisplayName("findByIdAndTenantId - Should prevent cross-tenant access")
    void findByIdAndTenantId_PreventsCrossTenantAccess() {
        Order savedForTenantA = entityManager.persistFlushFind(
                createOrder(TENANT_A, "buyer@example.com", new BigDecimal("50.00"), OrderStatus.PENDING));

        Optional<Order> result = orderRepository.findByIdAndTenantId(savedForTenantA.getId(), TENANT_B);

        assertFalse(result.isPresent(), "Cross-tenant access must return Optional.empty()");
    }

    @Test
    @DisplayName("findByIdAndTenantId - Should return empty when ID does not exist")
    void findByIdAndTenantId_ReturnsEmptyForRandomId() {
        Optional<Order> result = orderRepository.findByIdAndTenantId(UUID.randomUUID(), TENANT_A);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findByTenantIdAndStatus - Should filter orders by both tenant and order status")
    void findByTenantIdAndStatus_FiltersByBothFields() {
        entityManager.persist(createOrder(TENANT_A, "user1@example.com", new BigDecimal("15.00"), OrderStatus.PENDING));
        entityManager
                .persist(createOrder(TENANT_A, "user2@example.com", new BigDecimal("25.00"), OrderStatus.CANCELLED));
        entityManager.persist(createOrder(TENANT_B, "user3@example.com", new BigDecimal("35.00"), OrderStatus.PENDING));
        entityManager.flush();

        List<Order> matchingOrders = orderRepository.findByTenantIdAndStatus(TENANT_A, OrderStatus.PENDING);

        assertEquals(1, matchingOrders.size());
        Order order = matchingOrders.get(0);
        assertEquals(TENANT_A, order.getTenantId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals("user1@example.com", order.getCustomerEmail());
    }

    @Test
    @DisplayName("Audit timestamps and UUID generation should trigger upon persistence")
    void persistenceTriggersAutoGeneratedFields() {
        Order order = createOrder(TENANT_A, "user@example.com", new BigDecimal("100.00"), OrderStatus.PENDING);
        Order saved = entityManager.persistFlushFind(order);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(OrderStatus.PENDING, saved.getStatus());
    }
}
