package com.ecommerce.order_service.dto;

import com.ecommerce.order_service.model.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should set and get all fields correctly")
    void testGettersAndSetters() {
        UUID id = UUID.randomUUID();
        String tenantId = "tenant-alpha";
        String email = "shopper@example.com";
        BigDecimal amount = new BigDecimal("120.50");
        OrderStatus status = OrderStatus.values()[0];
        LocalDateTime now = LocalDateTime.now();

        OrderResponse response = new OrderResponse();
        response.setId(id);
        response.setTenantId(tenantId);
        response.setCustomerEmail(email);
        response.setTotalAmount(amount);
        response.setStatus(status);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        assertEquals(id, response.getId());
        assertEquals(tenantId, response.getTenantId());
        assertEquals(email, response.getCustomerEmail());
        assertEquals(amount, response.getTotalAmount());
        assertEquals(status, response.getStatus());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    @DisplayName("Should verify equals and hashCode contract")
    void testEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = new BigDecimal("50.00");
        OrderStatus status = OrderStatus.values()[0];

        OrderResponse res1 = new OrderResponse();
        res1.setId(id);
        res1.setTenantId("tenant-1");
        res1.setCustomerEmail("user@example.com");
        res1.setTotalAmount(amount);
        res1.setStatus(status);
        res1.setCreatedAt(now);
        res1.setUpdatedAt(now);

        OrderResponse res2 = new OrderResponse();
        res2.setId(id);
        res2.setTenantId("tenant-1");
        res2.setCustomerEmail("user@example.com");
        res2.setTotalAmount(amount);
        res2.setStatus(status);
        res2.setCreatedAt(now);
        res2.setUpdatedAt(now);

        OrderResponse res3 = new OrderResponse();
        res3.setId(UUID.randomUUID());
        res3.setTenantId("tenant-2");

        assertEquals(res1, res2);
        assertEquals(res1.hashCode(), res2.hashCode());
        assertNotEquals(res1, res3);
        assertNotEquals(res1, null);
        assertNotEquals(res1, new Object());
    }

    @Test
    @DisplayName("Should produce a descriptive toString representation")
    void testToString() {
        UUID id = UUID.randomUUID();
        OrderResponse response = new OrderResponse();
        response.setId(id);
        response.setCustomerEmail("buyer@example.com");

        String result = response.toString();

        assertTrue(result.contains("OrderResponse"));
        assertTrue(result.contains(id.toString()));
        assertTrue(result.contains("buyer@example.com"));
    }

    @Test
    @DisplayName("Should serialize to JSON and deserialize back to OrderResponse")
    void testJsonSerializationAndDeserialization() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 10, 6, 12, 0, 0);

        OrderResponse original = new OrderResponse();
        original.setId(id);
        original.setTenantId("tenant-123");
        original.setCustomerEmail("customer@domain.com");
        original.setTotalAmount(new BigDecimal("199.99"));
        original.setStatus(OrderStatus.values()[0]);
        original.setCreatedAt(now);
        original.setUpdatedAt(now);

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(original);
        assertNotNull(json);
        assertTrue(json.contains(id.toString()));
        assertTrue(json.contains("customer@domain.com"));

        // Deserialize back
        OrderResponse deserialized = objectMapper.readValue(json, OrderResponse.class);

        assertEquals(original, deserialized);
    }
}
