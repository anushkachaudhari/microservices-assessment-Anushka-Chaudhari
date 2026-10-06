package com.ecommerce.notification_service.service;

import com.ecommerce.notification_service.dto.NotificationRequest;
import com.ecommerce.notification_service.model.NotificationLog;
import com.ecommerce.notification_service.repository.NotificationRepository;
import com.ecommerce.notification_service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void processNotification(NotificationRequest request) {
        String tenantId = TenantContext.getTenantId();

        // Simulate sending a real notification (e.g., Email, SMS, Webhook)
        log.info("==================================================");
        log.info("[SIMULATED NOTIFICATION] Tenant: {}", tenantId);
        log.info("To Customer: {}", request.getCustomerEmail());
        log.info("Event Type: {}", request.getEventType());
        log.info("Order ID: {} | Amount: ${}", request.getOrderId(), request.getAmount());
        log.info("Status: Notification successfully dispatched.");
        log.info("==================================================");

        // Save audit record
        NotificationLog logEntry = NotificationLog.builder()
                .tenantId(tenantId)
                .orderId(request.getOrderId())
                .eventType(request.getEventType())
                .customerEmail(request.getCustomerEmail())
                .amount(request.getAmount())
                .simulationStatus("SIMULATED_SENT")
                .build();

        notificationRepository.save(logEntry);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationsForCurrentTenant() {
        return notificationRepository.findByTenantId(TenantContext.getTenantId());
    }
}