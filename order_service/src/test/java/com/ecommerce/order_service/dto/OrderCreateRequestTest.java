package com.ecommerce.order_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Should pass validation when all fields are valid")
    void validRequest_NoViolations() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail("buyer@example.com");
        request.setTotalAmount(new BigDecimal("29.99"));

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Expected no validation violations for valid input");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t", "\n" })
    @DisplayName("Should fail validation when customerEmail is blank, empty, or null")
    void invalidEmail_BlankOrNull(String email) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail(email);
        request.setTotalAmount(new BigDecimal("10.00"));

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("customerEmail")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "plainaddress", "missingatsign.com", "@missingusername.com", "user@" })
    @DisplayName("Should fail validation when customerEmail has an invalid format")
    void invalidEmail_Malformed(String email) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail(email);
        request.setTotalAmount(new BigDecimal("10.00"));

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("customerEmail") &&
                v.getMessage().equals("Invalid email format")));
    }

    @Test
    @DisplayName("Should fail validation when totalAmount is null")
    void invalidAmount_Null() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail("buyer@example.com");
        request.setTotalAmount(null);

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<OrderCreateRequest> violation = violations.iterator().next();
        assertEquals("totalAmount", violation.getPropertyPath().toString());
        assertEquals("Total amount is required", violation.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "0.00", "-0.01", "-100.00" })
    @DisplayName("Should fail validation when totalAmount is less than 0.01")
    void invalidAmount_BelowMinimum(String amount) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail("buyer@example.com");
        request.setTotalAmount(new BigDecimal(amount));

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("totalAmount") &&
                v.getMessage().equals("Total amount must be greater than zero")));
    }
}
