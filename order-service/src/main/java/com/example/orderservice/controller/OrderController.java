package com.example.orderservice.controller;

import lombok.RequiredArgsConstructor;
import com.example.orderservice.model.Order;
import com.example.orderservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam BigDecimal amount) {
        try {
            log.info("Received request to create order for user {} with amount {}", userId, amount);
            Order order = orderService.createOrder(userId, amount);
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            log.error("Failed to create order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while creating order: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ErrorResponse("Failed to create order: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    record ErrorResponse(String message) {}
} 