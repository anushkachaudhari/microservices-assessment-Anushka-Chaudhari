package com.ecommerce.order_service.config;

import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OperationCustomizer globalHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(
                    new Parameter()
                            .in("header")
                            .name("X-Tenant-ID")
                            .description("Tenant Identifier required by interceptor")
                            .required(true)
                            .schema(new StringSchema()._default("default-tenant"))
            );
            return operation;
        };
    }
}