package com.ecommerce.notification_service.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class NotificationRequest {
    private UUID orderId;
    private String eventType;
    private String customerEmail;
    private BigDecimal amount;
}