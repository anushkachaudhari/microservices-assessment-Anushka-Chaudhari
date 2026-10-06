package com.ecommerce.notification_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class NotificationServiceApplicationTest {

    @Test
    @DisplayName("Application context should load successfully with all bean wiring configurations")
    void contextLoads() {
    }

    @Test
    @DisplayName("Main entry-point method should execute and start up the application without errors")
    void mainMethodStartsApplication() {
        assertDoesNotThrow(() -> NotificationServiceApplication.main(new String[] { "--spring.profiles.active=test" }));
    }
}