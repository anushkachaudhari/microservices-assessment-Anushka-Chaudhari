package com.ecommerce.notification_service.repository;

import com.ecommerce.notification_service.model.NotificationLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    private NotificationLog createLog(String tenantId, String email, String eventType) {
        return NotificationLog.builder()
                .tenantId(tenantId)
                .orderId(UUID.randomUUID())
                .eventType(eventType)
                .customerEmail(email)
                .amount(new BigDecimal("125.50"))
                .simulationStatus("SIMULATED_SENT")
                .build();
    }

    @Test
    @DisplayName("findByTenantId - Should filter and return only records belonging to requested tenant")
    void findByTenantId_ReturnsOnlyMatchingTenantRecords() {
        entityManager.persist(createLog(TENANT_A, "buyer1@example.com", "ORDER_CREATED"));
        entityManager.persist(createLog(TENANT_A, "buyer2@example.com", "ORDER_COMPLETED"));
        entityManager.persist(createLog(TENANT_B, "buyer3@example.com", "ORDER_CREATED"));
        entityManager.flush();

        List<NotificationLog> tenantALogs = notificationRepository.findByTenantId(TENANT_A);

        assertEquals(2, tenantALogs.size(), "Should find exactly 2 records for tenant-alpha");
        assertTrue(tenantALogs.stream().allMatch(log -> TENANT_A.equals(log.getTenantId())),
                "All returned logs must belong to tenant-alpha");
    }

    @Test
    @DisplayName("findByTenantId - Should return empty collection when tenant has no dispatched records")
    void findByTenantId_ReturnsEmptyCollectionWhenNoMatchesExist() {
        entityManager.persist(createLog(TENANT_A, "buyer1@example.com", "ORDER_CREATED"));
        entityManager.flush();

        List<NotificationLog> logs = notificationRepository.findByTenantId("non-existent-tenant-id");

        assertTrue(logs.isEmpty(), "Expected an empty list for a tenant with no log entries");
    }

    @Test
    @DisplayName("JPA Mapping - Verification of automated ID assignment and entity persistence fields")
    void entityPersistence_AutoPopulatesFields() {
        NotificationLog individualLog = createLog(TENANT_A, "audit@example.com", "ORDER_CANCELLED");

        NotificationLog persistedLog = entityManager.persistFlushFind(individualLog);

        assertNotNull(persistedLog.getId(), "Primary Key UUID should be automatically generated upon persistence");
        assertEquals("ORDER_CANCELLED", persistedLog.getEventType());
    }
}
