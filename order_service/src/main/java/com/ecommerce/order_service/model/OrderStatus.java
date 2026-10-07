package com.ecommerce.order_service.model;

import java.util.Map;
import java.util.Set;
import java.util.EnumSet;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, EnumSet.of(PROCESSING, COMPLETED, CANCELLED),
            PROCESSING, EnumSet.of(COMPLETED, CANCELLED),
            COMPLETED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class));

    /**
     * @return true if an order in this status can be updated.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)).contains(target);
    }
}