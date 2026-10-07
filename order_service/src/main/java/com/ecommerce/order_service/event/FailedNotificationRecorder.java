package com.ecommerce.order_service.event;

import com.ecommerce.order_service.model.FailedNotification;
import com.ecommerce.order_service.repository.FailedNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FailedNotificationRecorder {

    private final FailedNotificationRepository failedNotificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OrderEvent event, String failureReason) {
        failedNotificationRepository.save(FailedNotification.builder()
                .tenantId(event.tenantId())
                .orderId(event.orderId())
                .eventType(event.eventType())
                .customerEmail(event.customerEmail())
                .amount(event.amount())
                .failureReason(failureReason)
                .build());
    }
}