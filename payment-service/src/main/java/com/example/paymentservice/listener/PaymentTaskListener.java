package com.example.paymentservice.listener;

import lombok.RequiredArgsConstructor;
import com.example.paymentservice.model.PaymentTask;
import com.example.paymentservice.service.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTaskListener {
    private final PaymentService paymentService;

    @RabbitListener(queues = "payment.task.queue")
    @Transactional
    public void handlePaymentTask(PaymentTask task, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            paymentService.processPaymentTask(task);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Error processing payment task: {}", e.getMessage(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (IOException ex) {
                log.error("Error sending NACK: {}", ex.getMessage(), ex);
            }
        }
    }
} 