package com.example.orderservice.controller;

import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private UUID userId;
    private UUID orderId;
    private Order order;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        amount = new BigDecimal("100.00");
        
        order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void createOrder_ShouldReturnCreatedOrder() {
        when(orderService.createOrder(userId, amount)).thenReturn(order);

        ResponseEntity<?> response = orderController.createOrder(userId, amount);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(order, response.getBody());
        verify(orderService).createOrder(userId, amount);
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenServiceThrowsIllegalStateException() {
        when(orderService.createOrder(userId, amount))
            .thenThrow(new IllegalStateException("Invalid order"));

        ResponseEntity<?> response = orderController.createOrder(userId, amount);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof OrderController.ErrorResponse);
        assertEquals("Invalid order", ((OrderController.ErrorResponse) response.getBody()).message());
        verify(orderService).createOrder(userId, amount);
    }

    @Test
    void createOrder_ShouldReturnInternalServerError_WhenServiceThrowsException() {
        when(orderService.createOrder(userId, amount))
            .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<?> response = orderController.createOrder(userId, amount);

        assertNotNull(response);
        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof OrderController.ErrorResponse);
        assertTrue(((OrderController.ErrorResponse) response.getBody()).message().contains("Failed to create order"));
        verify(orderService).createOrder(userId, amount);
    }

    @Test
    void getUserOrders_ShouldReturnUserOrders() {
        List<Order> expectedOrders = Arrays.asList(order);
        when(orderService.getUserOrders(userId)).thenReturn(expectedOrders);

        ResponseEntity<List<Order>> response = orderController.getUserOrders(userId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expectedOrders, response.getBody());
        verify(orderService).getUserOrders(userId);
    }

    @Test
    void getOrder_ShouldReturnOrder() {
        when(orderService.getOrder(orderId)).thenReturn(order);

        ResponseEntity<Order> response = orderController.getOrder(orderId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(order, response.getBody());
        verify(orderService).getOrder(orderId);
    }
} 