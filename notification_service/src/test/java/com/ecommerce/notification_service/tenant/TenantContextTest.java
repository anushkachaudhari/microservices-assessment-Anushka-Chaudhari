package com.ecommerce.notification_service.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantContextTest {

    @BeforeEach
    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should set and retrieve tenantId on the current thread")
    void setAndGetTenantId_CurrentThread() {
        String expectedTenant = "tenant-alpha-123";

        TenantContext.setTenantId(expectedTenant);

        assertEquals(expectedTenant, TenantContext.getTenantId());
    }

    @Test
    @DisplayName("clear() should remove tenantId from the current thread")
    void clear_RemovesTenantId() {
        TenantContext.setTenantId("tenant-to-remove");
        assertEquals("tenant-to-remove", TenantContext.getTenantId());

        TenantContext.clear();

        assertNull(TenantContext.getTenantId(), "TenantId must be null after clearing context");
    }

    @Test
    @DisplayName("Should maintain thread isolation (separate threads have separate tenant contexts)")
    void threadIsolation_DifferentThreadsHaveIndependentContext() throws ExecutionException, InterruptedException {
        String mainThreadTenant = "main-thread-tenant";
        String workerThreadTenant = "worker-thread-tenant";

        TenantContext.setTenantId(mainThreadTenant);

        CompletableFuture<String> workerFuture = CompletableFuture.supplyAsync(() -> {
            String initialTenant = TenantContext.getTenantId();
            TenantContext.setTenantId(workerThreadTenant);
            String assignedTenant = TenantContext.getTenantId();
            TenantContext.clear();
            return initialTenant == null ? assignedTenant : "LEAKED";
        });

        String workerResult = workerFuture.get();

        assertEquals(workerThreadTenant, workerResult);
        assertEquals(mainThreadTenant, TenantContext.getTenantId());
    }
}
