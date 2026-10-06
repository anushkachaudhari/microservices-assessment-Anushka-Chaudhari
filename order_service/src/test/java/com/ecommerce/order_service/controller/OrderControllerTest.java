package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.model.OrderStatus;
import com.ecommerce.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TEST_TENANT = "test-tenant-id";

    // -------------------------------------------------------------------------
    // POST /api/orders (Create Order)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/orders - Should create order and return 201 Created when payload is valid")
    void createOrder_Success() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail("customer@example.com");
        request.setTotalAmount(new BigDecimal("49.99"));

        OrderResponse mockResponse = new OrderResponse();

        when(orderService.createOrder(any(OrderCreateRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/orders")
                .header(TENANT_HEADER, TEST_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(orderService).createOrder(any(OrderCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/orders - Should return 400 Bad Request when customerEmail is invalid")
    void createOrder_InvalidEmail_ReturnsBadRequest() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail("not-a-valid-email");
        request.setTotalAmount(new BigDecimal("49.99"));

        mockMvc.perform(post("/api/orders")
                .header(TENANT_HEADER, TEST_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any());
    }

    @Test
    @DisplayName("POST /api/orders - Should return 400 Bad Request when totalAmount is null")
    void createOrder_NullAmount_ReturnsBadRequest() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail("customer@example.com");
        request.setTotalAmount(null);

        mockMvc.perform(post("/api/orders")
                .header(TENANT_HEADER, TEST_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any());
    }

    @Test
    @DisplayName("POST /api/orders - Should return 400 Bad Request when totalAmount is zero or negative")
    void createOrder_InvalidAmount_ReturnsBadRequest() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setCustomerEmail("customer@example.com");
        request.setTotalAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/orders")
                .header(TENANT_HEADER, TEST_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any());
    }

    // -------------------------------------------------------------------------
    // GET /api/orders (Get All Orders)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/orders - Should return list of orders and 200 OK")
    void getAllOrders_Success() throws Exception {
        OrderResponse mockResponse = new OrderResponse();
        List<OrderResponse> orders = List.of(mockResponse);

        when(orderService.getOrdersForCurrentTenant()).thenReturn(orders);

        mockMvc.perform(get("/api/orders")
                .header(TENANT_HEADER, TEST_TENANT)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(orderService).getOrdersForCurrentTenant();
    }

    @Test
    @DisplayName("GET /api/orders - Should return empty list and 200 OK when no orders exist")
    void getAllOrders_EmptyList() throws Exception {
        when(orderService.getOrdersForCurrentTenant()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/orders")
                .header(TENANT_HEADER, TEST_TENANT)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(orderService).getOrdersForCurrentTenant();
    }

    // -------------------------------------------------------------------------
    // GET /api/orders/{id} (Get Order by ID)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/orders/{id} - Should return order and 200 OK when ID exists")
    void getOrderById_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse mockResponse = new OrderResponse();

        when(orderService.getOrderById(orderId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/orders/{id}", orderId)
                .header(TENANT_HEADER, TEST_TENANT)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(orderService).getOrderById(orderId);
    }

    // -------------------------------------------------------------------------
    // PUT /api/orders/{id}/status (Update Order Status)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Should update status and return 200 OK")
    void updateOrderStatus_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderStatus targetStatus = OrderStatus.COMPLETED;
        OrderResponse mockResponse = new OrderResponse();

        when(orderService.updateOrderStatus(eq(orderId), eq(targetStatus))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/orders/{id}/status", orderId)
                .header(TENANT_HEADER, TEST_TENANT)
                .param("status", targetStatus.name())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(orderService).updateOrderStatus(orderId, targetStatus);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Should return 400 Bad Request when status parameter is missing")
    void updateOrderStatus_MissingParam_ReturnsBadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(put("/api/orders/{id}/status", orderId)
                .header(TENANT_HEADER, TEST_TENANT)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).updateOrderStatus(any(), any());
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Should return 400 Bad Request when status parameter is invalid")
    void updateOrderStatus_InvalidStatus_ReturnsBadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(put("/api/orders/{id}/status", orderId)
                .header(TENANT_HEADER, TEST_TENANT)
                .param("status", "NON_EXISTENT_STATUS")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).updateOrderStatus(any(), any());
    }

    // -------------------------------------------------------------------------
    // POST /api/orders/{id}/cancel (Cancel Order)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/orders/{id}/cancel - Should cancel order and return 200 OK")
    void cancelOrder_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse mockResponse = new OrderResponse();
        when(orderService.cancelOrder(orderId)).thenReturn(mockResponse);
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                .header(TENANT_HEADER, TEST_TENANT)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(orderService).cancelOrder(orderId);
    }
}
