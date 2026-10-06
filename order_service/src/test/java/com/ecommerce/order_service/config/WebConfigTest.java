package com.ecommerce.order_service.config;

import com.ecommerce.order_service.tenant.TenantInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebConfigTest {

    @Mock
    private TenantInterceptor tenantInterceptor;

    @Mock
    private InterceptorRegistry interceptorRegistry;

    @Mock
    private InterceptorRegistration interceptorRegistration;

    @InjectMocks
    private WebConfig webConfig;

    @Test
    @DisplayName("Should register TenantInterceptor with /api/** path pattern")
    void addInterceptors_RegistersTenantInterceptorWithApiPath() {
        when(interceptorRegistry.addInterceptor(tenantInterceptor))
                .thenReturn(interceptorRegistration);

        webConfig.addInterceptors(interceptorRegistry);

        verify(interceptorRegistry).addInterceptor(tenantInterceptor);
        verify(interceptorRegistration).addPathPatterns("/api/**");
    }
}
