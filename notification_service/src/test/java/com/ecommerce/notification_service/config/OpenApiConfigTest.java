package com.ecommerce.notification_service.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;
    private OperationCustomizer customizer;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
        customizer = openApiConfig.globalHeaderCustomizer();
    }

    @Test
    @DisplayName("globalHeaderCustomizer bean should not be null")
    void beanInitialization() {
        assertNotNull(customizer, "OperationCustomizer bean must be created");
    }

    @Test
    @DisplayName("Should add required X-Tenant-ID header parameter with default schema to Operation")
    void customize_AddsTenantHeaderParameter() {
        Operation operation = new Operation();
        Operation result = customizer.customize(operation, null);

        assertNotNull(result);
        assertNotNull(result.getParameters());
        assertEquals(1, result.getParameters().size());

        Parameter parameter = result.getParameters().get(0);
        assertEquals("header", parameter.getIn());
        assertEquals("X-Tenant-ID", parameter.getName());
        assertEquals("Tenant Identifier required by interceptor", parameter.getDescription());
        assertTrue(parameter.getRequired());

        assertNotNull(parameter.getSchema());
        assertInstanceOf(StringSchema.class, parameter.getSchema());
        assertEquals("default-tenant", parameter.getSchema().getDefault());
    }

    @Test
    @DisplayName("Should append X-Tenant-ID parameter without clearing existing parameters")
    void customize_PreservesExistingParameters() {
        Operation operation = new Operation();
        operation.addParametersItem(new Parameter().name("existingParam").in("query"));

        Operation result = customizer.customize(operation, null);

        assertEquals(2, result.getParameters().size());
        assertEquals("existingParam", result.getParameters().get(0).getName());
        assertEquals("X-Tenant-ID", result.getParameters().get(1).getName());
    }
}
