package com.ecommerce.order_service.event;

import com.ecommerce.order_service.client.NotificationRestClient;
import com.ecommerce.order_service.model.FailedNotification;
import com.ecommerce.order_service.repository.FailedNotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderNotificationListenerTest {

    @Mock
    private NotificationRestClient notificationClient;

    @Mock
    private FailedNotificationRepository failedNotificationRepository;

    @InjectMocks
    private OrderNotificationListener listener;

    private OrderEvent event() {
        return new OrderEvent(UUID.randomUUID(), "tenant-acme", "ORDER_CREATED",
                "buyer@example.com", new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("On successful delivery, no failure record is written")
    void onOrderEvent_success_noFailurePersisted() {
        OrderEvent event = event();
        doNothing().when(notificationClient)
                .sendNotification(any(), anyString(), anyString(), anyString(), any());

        listener.onOrderEvent(event);

        verify(notificationClient).sendNotification(
                eq(event.orderId()), eq(event.tenantId()), eq(event.eventType()),
                eq(event.customerEmail()), eq(event.amount()));
        verify(failedNotificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("When delivery fails, a durable FailedNotification is persisted for reconciliation")
    void onOrderEvent_failure_persistsFailure() {
        OrderEvent event = event();
        doThrow(new RuntimeException("connection refused"))
                .when(notificationClient).sendNotification(any(), anyString(), anyString(), anyString(), any());

        listener.onOrderEvent(event);

        ArgumentCaptor<FailedNotification> captor = ArgumentCaptor.forClass(FailedNotification.class);
        verify(failedNotificationRepository).save(captor.capture());
        FailedNotification saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(event.orderId());
        assertThat(saved.getTenantId()).isEqualTo(event.tenantId());
        assertThat(saved.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(saved.getFailureReason()).contains("connection refused");
    }
}
