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

    @Transactional
    public Order createOrder(UUID userId, BigDecimal amount) {
        log.info("Creating order for user {} with amount {}", userId, amount);
        
        // Проверяем существование аккаунта в payment-service
        try {
            log.info("Checking account existence for user {} in payment-service", userId);
            Boolean accountExists = restTemplate.getForObject(
                "http://payment-service/api/accounts/check/{userId}",
                Boolean.class,
                userId
            );
            
            log.info("Account existence check result for user {}: {}", userId, accountExists);
            
            if (accountExists == null || !accountExists) {
                String errorMessage = "Account not found for user: " + userId;
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = "Failed to verify account existence: " + e.getMessage();
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
        
        try {
            Order order = new Order();
            order.setUserId(userId);
            order.setAmount(amount);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order = orderRepository.save(order);
            log.info("Created order with id {} for user {}", order.getId(), userId);

            // Отправляем платежную задачу
            log.info("Sending payment task for order {}", order.getId());
            rabbitTemplate.convertAndSend("payment.exchange", "payment.task", 
                new PaymentTask(order.getId(), userId, amount));
            log.info("Payment task sent for order {}", order.getId());

            return order;
        } catch (Exception e) {
            String errorMessage = "Failed to create order: " + e.getMessage();
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getUserOrders(UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    @Transactional
    public void handlePaymentResult(UUID orderId, boolean success) {
        log.info("Handling payment result for order {}: success={}", orderId, success);
        
        Order order = orderRepository.findByIdWithLock(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        
        if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            order.setStatus(success ? OrderStatus.PAID : OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            log.info("Order {} status updated to {}", orderId, order.getStatus());
        }
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

    private record PaymentTask(UUID orderId, UUID userId, BigDecimal amount) {}
} 