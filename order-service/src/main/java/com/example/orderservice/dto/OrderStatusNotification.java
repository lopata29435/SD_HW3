package com.example.orderservice.dto;

import com.example.orderservice.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusNotification {
    private UUID orderId;
    private OrderStatus status;
    private String message;
    private LocalDateTime timestamp;
} 