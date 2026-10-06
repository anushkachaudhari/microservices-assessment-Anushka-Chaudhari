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

        // Set tenant on main thread
        TenantContext.setTenantId(mainThreadTenant);

        // Execute and verify on a separate asynchronous thread
        CompletableFuture<String> workerFuture = CompletableFuture.supplyAsync(() -> {
            // Must initially be null on the separate thread
            String initialTenant = TenantContext.getTenantId();
            TenantContext.setTenantId(workerThreadTenant);
            String assignedTenant = TenantContext.getTenantId();
            TenantContext.clear();
            return initialTenant == null ? assignedTenant : "LEAKED";
        });

        String workerResult = workerFuture.get();

        // Worker thread should have had isolated access
        assertEquals(workerThreadTenant, workerResult);

        // Main thread tenant must remain unaffected by worker thread execution or
        // clearing
        assertEquals(mainThreadTenant, TenantContext.getTenantId());
    }
}
