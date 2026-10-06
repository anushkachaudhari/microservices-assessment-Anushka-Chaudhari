package com.ecommerce.notification_service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should set and get all fields correctly via Lombok")
    void testGettersAndSetters() {
        UUID orderId = UUID.randomUUID();
        String eventType = "ORDER_CREATED";
        String email = "customer@example.com";
        BigDecimal amount = new BigDecimal("250.75");

        NotificationRequest request = new NotificationRequest();
        request.setOrderId(orderId);
        request.setEventType(eventType);
        request.setCustomerEmail(email);
        request.setAmount(amount);

        assertEquals(orderId, request.getOrderId());
        assertEquals(eventType, request.getEventType());
        assertEquals(email, request.getCustomerEmail());
        assertEquals(amount, request.getAmount());
    }

    @Test
    @DisplayName("Should verify equals and hashCode contracts")
    void testEqualsAndHashCode() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("19.99");

        NotificationRequest req1 = new NotificationRequest();
        req1.setOrderId(orderId);
        req1.setEventType("ORDER_CANCELLED");
        req1.setCustomerEmail("user@example.com");
        req1.setAmount(amount);

        NotificationRequest req2 = new NotificationRequest();
        req2.setOrderId(orderId);
        req2.setEventType("ORDER_CANCELLED");
        req2.setCustomerEmail("user@example.com");
        req2.setAmount(amount);

        NotificationRequest req3 = new NotificationRequest();
        req3.setOrderId(UUID.randomUUID());
        req3.setEventType("ORDER_CREATED");

        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        assertNotEquals(req1, req3);
        assertNotEquals(req1, null);
        assertNotEquals(req1, new Object());
    }

    @Test
    @DisplayName("Should produce a detailed toString representation")
    void testToString() {
        UUID orderId = UUID.randomUUID();
        NotificationRequest request = new NotificationRequest();
        request.setOrderId(orderId);
        request.setEventType("ORDER_PAID");

        String result = request.toString();

        assertTrue(result.contains("NotificationRequest"));
        assertTrue(result.contains(orderId.toString()));
        assertTrue(result.contains("ORDER_PAID"));
    }

    @Test
    @DisplayName("Should serialize to JSON and deserialize cleanly back to object data")
    void testJsonSerializationAndDeserialization() throws Exception {
        UUID orderId = UUID.randomUUID();

        NotificationRequest original = new NotificationRequest();
        original.setOrderId(orderId);
        original.setEventType("ORDER_COMPLETED");
        original.setCustomerEmail("buyer@domain.com");
        original.setAmount(new BigDecimal("500.00"));

        // Object -> JSON String
        String jsonString = objectMapper.writeValueAsString(original);
        assertNotNull(jsonString);
        assertTrue(jsonString.contains(orderId.toString()));
        assertTrue(jsonString.contains("ORDER_COMPLETED"));

        // JSON String -> Object
        NotificationRequest deserialized = objectMapper.readValue(jsonString, NotificationRequest.class);

        assertEquals(original, deserialized);
        assertEquals(original.getOrderId(), deserialized.getOrderId());
        assertEquals(original.getAmount(), deserialized.getAmount());
    }
}
