package com.ecommerce.order_service.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderEvent(
        UUID orderId,
        String tenantId,
        String eventType,
        String customerEmail,
        BigDecimal amount) {
}
