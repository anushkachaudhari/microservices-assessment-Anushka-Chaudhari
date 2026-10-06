package com.ecommerce.order_service.client;

import com.ecommerce.order_service.tenant.TenantContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class NotificationRestClient {

    private final RestClient restClient;

    public NotificationRestClient(
            @Value("${notification.service.url:http://notification-service:8081}") String notificationServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(notificationServiceUrl)
                .build();
    }

    public void sendNotification(UUID orderId, String eventType, String customerEmail, BigDecimal amount) {
        String currentTenant = TenantContext.getTenantId();

        NotificationRequest request = new NotificationRequest(orderId, eventType, customerEmail, amount);

        try {
            restClient.post()
                    .uri("api/notifications")
                    .header("X-Tenant-ID", currentTenant)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully sent notification event [{}] for order [{}] under tenant [{}]", eventType, orderId,
                    currentTenant);
        } catch (Exception e) {
            log.error("Failed to send notification for order [{}]: {}", orderId, e.getMessage());
        }
    }

    @Data
    public static class NotificationRequest {
        @JsonProperty("orderId")
        private final UUID orderId;
        @JsonProperty("eventType")
        private final String eventType;
        @JsonProperty("customerEmail")
        private final String customerEmail;
        @JsonProperty("amount")
        private final BigDecimal amount;
    }
}