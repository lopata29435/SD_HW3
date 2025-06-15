package com.example.paymentservice.service;

import com.example.paymentservice.model.PaymentTask;
import com.example.paymentservice.model.PaymentTaskStatus;
import com.example.paymentservice.model.PaymentResultOutbox;
import com.example.paymentservice.repository.PaymentTaskRepository;
import com.example.paymentservice.repository.PaymentResultOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private PaymentTaskRepository paymentTaskRepository;

    @Mock
    private PaymentResultOutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;
    private UUID userId;
    private PaymentTask paymentTask;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        paymentTask = new PaymentTask();
        paymentTask.setOrderId(orderId);
        paymentTask.setUserId(userId);
        paymentTask.setAmount(new BigDecimal("100.00"));
        paymentTask.setStatus(PaymentTaskStatus.PENDING);
    }

    @Test
    void processPaymentTask_ShouldCompletePayment_WhenSufficientFunds() {
        when(accountService.getBalance(userId)).thenReturn(new BigDecimal("200.00"));
        when(paymentTaskRepository.save(any(PaymentTask.class))).thenReturn(paymentTask);
        when(outboxRepository.save(any(PaymentResultOutbox.class))).thenReturn(new PaymentResultOutbox());

        paymentService.processPaymentTask(paymentTask);

        assertEquals(PaymentTaskStatus.COMPLETED, paymentTask.getStatus());
        verify(accountService).deposit(userId, paymentTask.getAmount().negate());
        verify(paymentTaskRepository).save(paymentTask);
        verify(outboxRepository).save(any(PaymentResultOutbox.class));
    }

    @Test
    void processPaymentTask_ShouldFailPayment_WhenInsufficientFunds() {
        when(accountService.getBalance(userId)).thenReturn(new BigDecimal("50.00"));
        when(paymentTaskRepository.save(any(PaymentTask.class))).thenReturn(paymentTask);
        when(outboxRepository.save(any(PaymentResultOutbox.class))).thenReturn(new PaymentResultOutbox());

        paymentService.processPaymentTask(paymentTask);

        assertEquals(PaymentTaskStatus.FAILED, paymentTask.getStatus());
        assertEquals("Insufficient funds", paymentTask.getErrorMessage());
        verify(accountService, never()).deposit(any(), any());
        verify(paymentTaskRepository).save(paymentTask);
        verify(outboxRepository).save(any(PaymentResultOutbox.class));
    }

    @Test
    void processPaymentTask_ShouldFailPayment_WhenExceptionOccurs() {
        when(accountService.getBalance(userId)).thenThrow(new RuntimeException("Database error"));
        when(paymentTaskRepository.save(any(PaymentTask.class))).thenReturn(paymentTask);
        when(outboxRepository.save(any(PaymentResultOutbox.class))).thenReturn(new PaymentResultOutbox());

        paymentService.processPaymentTask(paymentTask);

        assertEquals(PaymentTaskStatus.FAILED, paymentTask.getStatus());
        assertEquals("Database error", paymentTask.getErrorMessage());
        verify(accountService, never()).deposit(any(), any());
        verify(paymentTaskRepository).save(paymentTask);
        verify(outboxRepository).save(any(PaymentResultOutbox.class));
    }
} 