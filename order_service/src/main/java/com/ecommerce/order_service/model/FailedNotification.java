package com.ecommerce.order_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification that could not be delivered to notification-service.
 */
@Entity
@Table(name = "failed_notifications", indexes = {
        @Index(name = "idx_failed_notification_tenant", columnList = "tenantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 1000)
    private String failureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime failedAt;
}
