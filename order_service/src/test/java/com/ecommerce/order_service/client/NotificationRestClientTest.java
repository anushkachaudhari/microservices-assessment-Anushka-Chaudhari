package com.ecommerce.order_service.client;

import com.ecommerce.order_service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockStatic;
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
    private MockedStatic<TenantContext> mockedTenantContext;

    @BeforeEach
    void setUp() {
        client = new NotificationRestClient(BASE_URL);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(client, "restClient", builder.build());

        mockedTenantContext = mockStatic(TenantContext.class);
    }

    @AfterEach
    void tearDown() {
        if (mockedTenantContext != null) {
            mockedTenantContext.close();
        }
    }

    @Test
    @DisplayName("Should send notification successfully with correct URI, headers, and payload")
    void sendNotification_Success() {
        UUID orderId = UUID.randomUUID();
        String tenantId = "tenant-acme-123";
        mockedTenantContext.when(TenantContext::getTenantId).thenReturn(tenantId);

        mockServer.expect(requestTo(BASE_URL + "/api/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Tenant-ID", tenantId))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.eventType").value("ORDER_CREATED"))
                .andExpect(jsonPath("$.customerEmail").value("buyer@example.com"))
                .andExpect(jsonPath("$.amount").value(99.99))
                .andRespond(withSuccess());

        client.sendNotification(orderId, "ORDER_CREATED", "buyer@example.com", new BigDecimal("99.99"));

        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle downstream server errors gracefully without throwing an exception")
    void sendNotification_ServerError_HandledGracefully() {
        UUID orderId = UUID.randomUUID();
        mockedTenantContext.when(TenantContext::getTenantId).thenReturn("tenant-xyz");

        mockServer.expect(requestTo(BASE_URL + "/api/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // catches all exceptions and logs an error
        assertDoesNotThrow(
                () -> client.sendNotification(orderId, "ORDER_PAID", "buyer@example.com", new BigDecimal("49.50")));

        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle null tenant context gracefully")
    void sendNotification_NullTenant_SentSuccessfully() {
        UUID orderId = UUID.randomUUID();
        mockedTenantContext.when(TenantContext::getTenantId).thenReturn(null);

        mockServer.expect(requestTo(BASE_URL + "/api/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        assertDoesNotThrow(
                () -> client.sendNotification(orderId, "ORDER_CANCELLED", "buyer@example.com", BigDecimal.ZERO));

        mockServer.verify();
    }
}
