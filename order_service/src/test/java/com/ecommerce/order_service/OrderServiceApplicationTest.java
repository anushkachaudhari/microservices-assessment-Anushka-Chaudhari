package com.ecommerce.order_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class OrderServiceApplicationTest {

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
    }

    @Test
    @DisplayName("Main method should execute without throwing exceptions")
    void mainMethodStartsApplication() {
        assertDoesNotThrow(() -> OrderServiceApplication.main(new String[] { "--spring.profiles.active=test" }));
    }
}
