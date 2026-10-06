package com.ecommerce.order_service.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should handle EntityNotFoundException and return 404 with ErrorResponse")
    void handleEntityNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/test/entity-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Order not found with id: 123"))
                .andExpect(jsonPath("$.path").value("/test/entity-not-found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException and return 400 with ErrorResponse")
    void handleIllegalArgument_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid order status"))
                .andExpect(jsonPath("$.path").value("/test/illegal-argument"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Should handle IllegalStateException and return 400 with ErrorResponse")
    void handleIllegalState_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/illegal-state"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Order is already cancelled"))
                .andExpect(jsonPath("$.path").value("/test/illegal-state"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException and return 400 with field errors")
    void handleValidationExceptions_ReturnsValidationErrorsMap() throws Exception {
        String invalidJsonPayload = "{\"name\":\"\"}";

        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.messages.name").value("Name is required"))
                .andExpect(jsonPath("$.path").value("/test/validation"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Should handle generic uncaught exceptions and return 500 with ErrorResponse")
    void handleGlobalException_ReturnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test/unhandled-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message", containsString("Database connection dropped")))
                .andExpect(jsonPath("$.path").value("/test/unhandled-exception"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // Test controller and payload to trigger each exception type
    // -------------------------------------------------------------------------

    @RestController
    static class TestController {

        @GetMapping("/test/entity-not-found")
        public void throwEntityNotFound() {
            throw new EntityNotFoundException("Order not found with id: 123");
        }

        @GetMapping("/test/illegal-argument")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid order status");
        }

        @GetMapping("/test/illegal-state")
        public void throwIllegalState() {
            throw new IllegalStateException("Order is already cancelled");
        }

        @GetMapping("/test/unhandled-exception")
        public void throwUnhandledException() {
            throw new RuntimeException("Database connection dropped");
        }

        @PostMapping("/test/validation")
        public void triggerValidation(@Valid @RequestBody DummyRequest request) {
            // No-op - validation fails before reaching body
        }
    }

    @Data
    static class DummyRequest {
        @NotBlank(message = "Name is required")
        private String name;
    }
}
