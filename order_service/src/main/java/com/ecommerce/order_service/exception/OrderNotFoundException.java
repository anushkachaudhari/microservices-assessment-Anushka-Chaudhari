package com.ecommerce.order_service.exception;

import java.util.UUID;

/**
 * Thrown when an order cannot be found within the current scope.
 * Maps to HTTP 404.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super("Order not found with ID: " + orderId);
    }
}
