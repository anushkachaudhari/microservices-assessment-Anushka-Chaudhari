package com.ecommerce.order_service.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class NotificationRestClient {

    private final RestClient restClient;

    public NotificationRestClient(
            @Value("${notification.service.url:http://notification-service:8081}") String notificationServiceUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .baseUrl(notificationServiceUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * Sends a notification event downstream.
     */
    public void sendNotification(UUID orderId, String tenantId, String eventType, String customerEmail,
            BigDecimal amount) {
        NotificationRequest request = new NotificationRequest(orderId, eventType, customerEmail, amount);

        restClient.post()
                .uri("/api/notifications")
                .header("X-Tenant-ID", tenantId)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.info("Successfully sent notification event [{}] for order [{}] under tenant [{}]",
                eventType, orderId, tenantId);
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
