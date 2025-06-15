package com.example.orderservice.service;

import com.example.orderservice.dto.OrderStatusNotification;
import com.example.orderservice.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    public void sendOrderStatusNotification(UUID orderId, OrderStatus status) {
        String message = switch (status) {
            case CREATED -> "Заказ создан";
            case PAYMENT_PENDING -> "Ожидание оплаты";
            case PAID -> "Заказ оплачен";
            case PAYMENT_FAILED -> "Ошибка оплаты";
        };

        OrderStatusNotification notification = new OrderStatusNotification(
            orderId,
            status,
            message,
            LocalDateTime.now()
        );

        // Отправляем уведомление на общий топик
        String destination = "/topic/orders";
        log.info("Отправка уведомления о статусе заказа {}: {}", orderId, status);
        messagingTemplate.convertAndSend(destination, notification);
    }
} 