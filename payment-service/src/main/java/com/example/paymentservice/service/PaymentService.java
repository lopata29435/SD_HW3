package com.example.paymentservice.service;

import com.example.paymentservice.model.PaymentTask;
import com.example.paymentservice.model.PaymentTaskStatus;
import com.example.paymentservice.repository.PaymentTaskRepository;
import com.example.paymentservice.repository.PaymentResultOutboxRepository;
import com.example.paymentservice.model.PaymentResultOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final AccountService accountService;
    private final PaymentTaskRepository paymentTaskRepository;
    private final PaymentResultOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void processPaymentTask(PaymentTask task) {
        log.info("Processing payment task for order {}: amount={}, userId={}", 
            task.getOrderId(), task.getAmount(), task.getUserId());
        
        try {
            // Проверяем баланс
            BigDecimal balance = accountService.getBalance(task.getUserId());
            log.info("Current balance for user {}: {}", task.getUserId(), balance);
            
            if (balance.compareTo(task.getAmount()) < 0) {
                log.warn("Insufficient funds for user {}: required={}, available={}", 
                    task.getUserId(), task.getAmount(), balance);
                task.setStatus(PaymentTaskStatus.FAILED);
                task.setErrorMessage("Insufficient funds");
                paymentTaskRepository.save(task);
                createOutboxRecord(task.getOrderId(), false);
                return;
            }

            // Списываем средства
            log.info("Deducting amount {} from user {}", task.getAmount(), task.getUserId());
            accountService.deposit(task.getUserId(), task.getAmount().negate());
            
            // Обновляем статус задачи
            task.setStatus(PaymentTaskStatus.COMPLETED);
            paymentTaskRepository.save(task);
            log.info("Payment task completed for order {}", task.getOrderId());
            
            // Создаем запись в outbox
            createOutboxRecord(task.getOrderId(), true);
        } catch (Exception e) {
            log.error("Error processing payment task for order {}: {}", task.getOrderId(), e.getMessage(), e);
            task.setStatus(PaymentTaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            paymentTaskRepository.save(task);
            createOutboxRecord(task.getOrderId(), false);
        }
    }

    private void createOutboxRecord(UUID orderId, boolean success) {
        log.info("Creating outbox record for order {}: success={}", orderId, success);
        PaymentResultOutbox outbox = new PaymentResultOutbox();
        outbox.setOrderId(orderId);
        outbox.setSuccess(success);
        outbox.setProcessed(false);
        outboxRepository.save(outbox);
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processUnprocessedMessages() {
        log.info("Processing unprocessed outbox messages");
        outboxRepository.findByProcessedFalse().forEach(outbox -> {
            try {
                log.info("Sending payment result for order {}: success={}", 
                    outbox.getOrderId(), outbox.isSuccess());
                rabbitTemplate.convertAndSend("payment.exchange", "payment.result", 
                    new PaymentResult(outbox.getOrderId(), outbox.isSuccess()));
                outbox.setProcessed(true);
                outboxRepository.save(outbox);
                log.info("Payment result sent for order {}", outbox.getOrderId());
            } catch (Exception e) {
                log.error("Error sending payment result for order {}: {}", 
                    outbox.getOrderId(), e.getMessage(), e);
            }
        });
    }

    private record PaymentResult(UUID orderId, boolean success) {}
} 