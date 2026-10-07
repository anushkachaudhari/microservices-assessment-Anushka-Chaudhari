package com.ecommerce.order_service.event;

import com.ecommerce.order_service.client.NotificationRestClient;
import com.ecommerce.order_service.model.FailedNotification;
import com.ecommerce.order_service.repository.FailedNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifies notification-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final NotificationRestClient notificationClient;
    private final FailedNotificationRepository failedNotificationRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderEvent(OrderEvent event) {
        try {
            notificationClient.sendNotification(
                    event.orderId(), event.tenantId(), event.eventType(),
                    event.customerEmail(), event.amount());
        } catch (Exception ex) {
            log.error("Failed to deliver notification [{}] for order [{}] (tenant [{}]): {}. "
                    + "Recording for reconciliation.",
                    event.eventType(), event.orderId(), event.tenantId(), ex.getMessage());
            persistFailure(event, ex);
        }
    }

    private void persistFailure(OrderEvent event, Exception ex) {
        try {
            failedNotificationRepository.save(FailedNotification.builder()
                    .tenantId(event.tenantId())
                    .orderId(event.orderId())
                    .eventType(event.eventType())
                    .customerEmail(event.customerEmail())
                    .amount(event.amount())
                    .failureReason(truncate(ex.getMessage()))
                    .build());
        } catch (Exception persistEx) {
            log.error("Could not persist failed notification for order [{}]: {}",
                    event.orderId(), persistEx.getMessage());
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
