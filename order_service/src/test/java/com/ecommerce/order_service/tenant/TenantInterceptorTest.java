package com.ecommerce.order_service.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantInterceptorTest {

    private TenantInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Object handler;

    @BeforeEach
    void setUp() {
        interceptor = new TenantInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        handler = new Object();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // -------------------------------------------------------------------------
    // preHandle Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("preHandle - Should set TenantContext and return true when X-Tenant-ID header is present")
    void preHandle_ValidTenantHeader_SetsContextAndReturnsTrue() {
        String expectedTenant = "tenant-acme-123";
        request.addHeader("X-Tenant-ID", expectedTenant);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result, "preHandle should proceed when tenant header is present");
        assertEquals(expectedTenant, TenantContext.getTenantId());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", " ", "   ", "\t", "\n" })
    @DisplayName("preHandle - Should set 400 Bad Request and return false when X-Tenant-ID is null, empty, or whitespace")
    void preHandle_MissingOrBlankTenantHeader_ReturnsBadRequest(String invalidHeader) {
        if (invalidHeader != null) {
            request.addHeader("X-Tenant-ID", invalidHeader);
        }

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result, "preHandle should block requests missing a valid tenant header");
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertNull(TenantContext.getTenantId(), "TenantContext should not be set for invalid headers");
    }

    // -------------------------------------------------------------------------
    // afterCompletion Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("afterCompletion - Should clear TenantContext when request completes successfully")
    void afterCompletion_ClearsTenantContext() {
        TenantContext.setTenantId("tenant-to-clean");
        assertEquals("tenant-to-clean", TenantContext.getTenantId());

        interceptor.afterCompletion(request, response, handler, null);

        assertNull(TenantContext.getTenantId(), "TenantContext must be cleared after completion");
    }

    @Test
    @DisplayName("afterCompletion - Should clear TenantContext even when an exception occurred")
    void afterCompletion_WithException_ClearsTenantContext() {
        TenantContext.setTenantId("tenant-error-case");
        assertEquals("tenant-error-case", TenantContext.getTenantId());

        Exception sampleException = new RuntimeException("Downstream handler failed");
        interceptor.afterCompletion(request, response, handler, sampleException);

        assertNull(TenantContext.getTenantId(), "TenantContext must be cleared even if an exception was thrown");
    }
}
