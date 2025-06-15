package com.example.orderservice.service;

import lombok.RequiredArgsConstructor;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.model.PaymentResultOutbox;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentResultOutboxRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentResultOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;

    @Transactional
    public Order createOrder(UUID userId, BigDecimal amount) {
        log.info("Создание заказа для пользователя {} на сумму {}", userId, amount);
        
        // Проверяем существование аккаунта
        Boolean accountExists = restTemplate.getForObject(
            "http://payment-service/api/accounts/check/{userId}",
            Boolean.class,
            userId
        );

        if (accountExists == null || !accountExists) {
            throw new IllegalStateException("Аккаунт пользователя не найден");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(OrderStatus.CREATED);
        order = orderRepository.save(order);

        // Отправляем уведомление о создании заказа
        notificationService.sendOrderStatusNotification(order.getId(), OrderStatus.CREATED);

        // Отправляем задачу на оплату
        rabbitTemplate.convertAndSend(
            "payment.exchange",
            "payment.task",
            new PaymentTask(order.getId(), userId, amount)
        );

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order = orderRepository.save(order);

        // Отправляем уведомление об ожидании оплаты
        notificationService.sendOrderStatusNotification(order.getId(), OrderStatus.PAYMENT_PENDING);

        return order;
    }

    @Transactional(readOnly = true)
    public List<Order> getUserOrders(UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    @Transactional
    public void handlePaymentResult(UUID orderId, boolean success) {
        Order order = orderRepository.findByIdWithLock(orderId);
        if (order == null) {
            throw new RuntimeException("Заказ не найден");
        }

        if (success) {
            order.setStatus(OrderStatus.PAID);
            // Отправляем уведомление об успешной оплате
            notificationService.sendOrderStatusNotification(orderId, OrderStatus.PAID);
        } else {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            // Отправляем уведомление об ошибке оплаты
            notificationService.sendOrderStatusNotification(orderId, OrderStatus.PAYMENT_FAILED);
        }

        orderRepository.save(order);
    }

    @Transactional
    public void processOutboxMessages() {
        List<PaymentResultOutbox> unprocessedMessages = outboxRepository.findByProcessedFalse();
        
        for (PaymentResultOutbox message : unprocessedMessages) {
            try {
                message.setProcessed(true);
                outboxRepository.save(message);
            } catch (Exception e) {
                log.error("Error processing outbox message: {}", e.getMessage(), e);
            }
        }
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        log.info("Обновлен статус заказа {} на {}", orderId, status);
        return orderRepository.save(order);
    }

    private record PaymentTask(UUID orderId, UUID userId, BigDecimal amount) {}
} 