package com.ecommerce.order_service.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NotificationRestClientTest {

    private static final String BASE_URL = "http://notification-service:8081";

    private NotificationRestClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        client = new NotificationRestClient(BASE_URL);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(client, "restClient", builder.build());
    }

    @Test
    @DisplayName("Sends the notification with correct URI, tenant header, and payload")
    void sendNotification_success() {
        UUID orderId = UUID.randomUUID();
        String tenantId = "tenant-acme-123";

        mockServer.expect(requestTo(BASE_URL + "/api/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Tenant-ID", tenantId))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.eventType").value("ORDER_CREATED"))
                .andExpect(jsonPath("$.customerEmail").value("buyer@example.com"))
                .andExpect(jsonPath("$.amount").value(99.99))
                .andRespond(withSuccess());

        client.sendNotification(orderId, tenantId, "ORDER_CREATED", "buyer@example.com", new BigDecimal("99.99"));

        mockServer.verify();
    }

    @Test
    @DisplayName("Propagates downstream server errors so the caller can record a durable failure")
    void sendNotification_serverError_propagates() {
        UUID orderId = UUID.randomUUID();

        mockServer.expect(requestTo(BASE_URL + "/api/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(Exception.class, () -> client.sendNotification(
                orderId, "tenant-xyz", "ORDER_UPDATED", "buyer@example.com", new BigDecimal("49.50")));

        mockServer.verify();
    }
}
