package com.ecommerce.notification_service.service;

import com.ecommerce.notification_service.dto.NotificationRequest;
import com.ecommerce.notification_service.model.NotificationLog;
import com.ecommerce.notification_service.repository.NotificationRepository;
import com.ecommerce.notification_service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String TEST_TENANT = "tenant-notification-xyz";

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private MockedStatic<TenantContext> mockedTenantContext;

    @BeforeEach
    void setUp() {
        mockedTenantContext = mockStatic(TenantContext.class);
        mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TEST_TENANT);
    }

    @AfterEach
    void tearDown() {
        if (mockedTenantContext != null) {
            mockedTenantContext.close();
        }
    }

    // -------------------------------------------------------------------------
    // processNotification
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processNotification - Should build audit log correctly and save to repository under active tenant")
    void processNotification_Success() {
        UUID orderId = UUID.randomUUID();
        NotificationRequest request = new NotificationRequest();
        request.setOrderId(orderId);
        request.setEventType("ORDER_CREATED");
        request.setCustomerEmail("alert-buyer@example.com");
        request.setAmount(new BigDecimal("299.90"));

        notificationService.processNotification(request);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationRepository).save(logCaptor.capture());

        NotificationLog capturedLog = logCaptor.getValue();
        assertNotNull(capturedLog);
        assertEquals(TEST_TENANT, capturedLog.getTenantId());
        assertEquals(orderId, capturedLog.getOrderId());
        assertEquals("ORDER_CREATED", capturedLog.getEventType());
        assertEquals("alert-buyer@example.com", capturedLog.getCustomerEmail());
        assertEquals(new BigDecimal("299.90"), capturedLog.getAmount());
        assertEquals("SIMULATED_SENT", capturedLog.getSimulationStatus());
    }

    // -------------------------------------------------------------------------
    // getNotificationsForCurrentTenant
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getNotificationsForCurrentTenant - Should extract tenant ID from context and return filtered audit logs")
    void getNotificationsForCurrentTenant_ReturnsList() {
        NotificationLog auditLog1 = NotificationLog.builder()
                .tenantId(TEST_TENANT)
                .eventType("ORDER_CREATED")
                .build();
        NotificationLog auditLog2 = NotificationLog.builder()
                .tenantId(TEST_TENANT)
                .eventType("ORDER_COMPLETED")
                .build();

        when(notificationRepository.findByTenantId(TEST_TENANT)).thenReturn(List.of(auditLog1, auditLog2));

        List<NotificationLog> results = notificationService.getNotificationsForCurrentTenant();

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(notificationRepository).findByTenantId(TEST_TENANT);
    }
}
