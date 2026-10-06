package com.ecommerce.notification_service.controller;

import com.ecommerce.notification_service.dto.NotificationRequest;
import com.ecommerce.notification_service.model.NotificationLog;
import com.ecommerce.notification_service.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TEST_TENANT = "tenant-notification-123";

    // -------------------------------------------------------------------------
    // POST /api/notifications
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/notifications - Should return 201 Created and invoke processing service")
    void receiveOrderEvent_Success() throws Exception {
        NotificationRequest request = new NotificationRequest();
        request.setOrderId(UUID.randomUUID());
        request.setEventType("ORDER_CREATED");
        request.setCustomerEmail("shopper@example.com");
        request.setAmount(new BigDecimal("150.00"));

        doNothing().when(notificationService).processNotification(any(NotificationRequest.class));

        mockMvc.perform(post("/api/notifications")
                .header(TENANT_HEADER, TEST_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(notificationService).processNotification(any(NotificationRequest.class));
    }

    // -------------------------------------------------------------------------
    // GET /api/notifications
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/notifications - Should return 200 OK and matching list of audit logs")
    void getNotificationLogs_Success() throws Exception {
        NotificationLog mockLog = NotificationLog.builder()
                .id(UUID.randomUUID())
                .tenantId(TEST_TENANT)
                .eventType("ORDER_CREATED")
                .customerEmail("shopper@example.com")
                .amount(new BigDecimal("150.00"))
                .simulationStatus("SIMULATED_SENT")
                .build();

        when(notificationService.getNotificationsForCurrentTenant()).thenReturn(List.of(mockLog));

        mockMvc.perform(get("/api/notifications")
                .header(TENANT_HEADER, TEST_TENANT)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("ORDER_CREATED"))
                .andExpect(jsonPath("$[0].customerEmail").value("shopper@example.com"));

        verify(notificationService).getNotificationsForCurrentTenant();
    }

    @Test
    @DisplayName("GET /api/notifications - Should return 200 OK and empty array when no log data exists")
    void getNotificationLogs_EmptyList() throws Exception {
        when(notificationService.getNotificationsForCurrentTenant()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/notifications")
                .header(TENANT_HEADER, TEST_TENANT)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(notificationService).getNotificationsForCurrentTenant();
    }
}
