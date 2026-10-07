package com.ecommerce.order_service.exception;

/**
 * Thrown when a requested order status transition is not valid.
 * Maps to HTTP 409.
 */
public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(String message) {
        super(message);
    }
}

