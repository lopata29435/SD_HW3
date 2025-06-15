package com.example.orderservice.service;

import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.model.PaymentResultOutbox;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentResultOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentResultOutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

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
    void createOrder_ShouldCreateOrder_WhenAccountExists() {
        when(restTemplate.getForObject(anyString(), eq(Boolean.class), eq(userId))).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.createOrder(userId, amount);

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(rabbitTemplate).convertAndSend(eq("payment.exchange"), eq("payment.task"), any(Object.class));
        verify(notificationService).sendOrderStatusNotification(eq(order.getId()), eq(OrderStatus.CREATED));
        verify(notificationService).sendOrderStatusNotification(eq(order.getId()), eq(OrderStatus.PAYMENT_PENDING));
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(amount, result.getAmount());
        assertEquals(OrderStatus.PAYMENT_PENDING, result.getStatus());
        verify(restTemplate).getForObject(anyString(), eq(Boolean.class), eq(userId));
    }

    @Test
    void createOrder_ShouldThrowException_WhenAccountDoesNotExist() {
        when(restTemplate.getForObject(anyString(), eq(Boolean.class), eq(userId))).thenReturn(false);

        assertThrows(RuntimeException.class, () -> orderService.createOrder(userId, amount));
        verify(restTemplate).getForObject(anyString(), eq(Boolean.class), eq(userId));
        verify(orderRepository, never()).save(any(Order.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void getUserOrders_ShouldReturnUserOrders() {
        List<Order> expectedOrders = Arrays.asList(order);
        when(orderRepository.findByUserId(userId)).thenReturn(expectedOrders);

        List<Order> result = orderService.getUserOrders(userId);

        assertEquals(expectedOrders, result);
        verify(orderRepository).findByUserId(userId);
    }

    @Test
    void getOrder_ShouldReturnOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.getOrder(orderId);

        assertEquals(order, result);
        verify(orderRepository).findById(orderId);
    }

    @Test
    void getOrder_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.getOrder(orderId));
        verify(orderRepository).findById(orderId);
    }

    @Test
    void handlePaymentResult_ShouldUpdateOrderStatus_WhenPaymentSuccessful() {
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.handlePaymentResult(orderId, true);

        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository).findByIdWithLock(orderId);
        verify(orderRepository).save(order);
        verify(notificationService).sendOrderStatusNotification(eq(orderId), eq(OrderStatus.PAID));
    }

    @Test
    void handlePaymentResult_ShouldUpdateOrderStatus_WhenPaymentFailed() {
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.handlePaymentResult(orderId, false);

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        verify(orderRepository).findByIdWithLock(orderId);
        verify(orderRepository).save(order);
        verify(notificationService).sendOrderStatusNotification(eq(orderId), eq(OrderStatus.PAYMENT_FAILED));
    }

    @Test
    void handlePaymentResult_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findByIdWithLock(orderId)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> orderService.handlePaymentResult(orderId, true));
        verify(orderRepository).findByIdWithLock(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void processOutboxMessages_ShouldProcessUnprocessedMessages() {
        PaymentResultOutbox outbox = new PaymentResultOutbox();
        outbox.setProcessed(false);
        when(outboxRepository.findByProcessedFalse()).thenReturn(Arrays.asList(outbox));
        when(outboxRepository.save(any(PaymentResultOutbox.class))).thenReturn(outbox);

        orderService.processOutboxMessages();

        assertTrue(outbox.isProcessed());
        verify(outboxRepository).findByProcessedFalse();
        verify(outboxRepository).save(outbox);
    }
} 