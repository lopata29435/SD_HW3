package com.example.orderservice.listener;

import com.example.orderservice.dto.PaymentResult;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultListener {
    private final OrderService orderService;

    @RabbitListener(queues = "payment.result.queue")
    @Transactional
    public void handlePaymentResult(PaymentResult result) {
        log.info("Received payment result for order {}: success={}", result.getOrderId(), result.isSuccess());
        orderService.handlePaymentResult(result.getOrderId(), result.isSuccess());
    }
} 