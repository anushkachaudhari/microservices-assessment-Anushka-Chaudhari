package com.ecommerce.notification_service.controller;

import com.ecommerce.notification_service.dto.NotificationRequest;
import com.ecommerce.notification_service.model.NotificationLog;
import com.ecommerce.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Receive an order event and process the notification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Notification processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public ResponseEntity<Void> receiveOrderEvent(@Valid @RequestBody NotificationRequest request) {
        notificationService.processNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Retrieve notification logs for the current tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved notification logs")
    })
    @GetMapping
    public ResponseEntity<List<NotificationLog>> getNotificationLogs() {
        List<NotificationLog> logs = notificationService.getNotificationsForCurrentTenant();
        return ResponseEntity.ok(logs);
    }
}