package com.example.orderservice.controller;

import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.model.OrderStatusUpdate;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OrderWebSocketController {

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/orders/update")
    @SendTo("/topic/orders")
    public OrderStatusUpdate updateOrderStatus(OrderStatusUpdate update) {
        log.info("Получено обновление статуса заказа: {}", update);
        Order order = orderService.updateOrderStatus(update.getOrderId(), update.getStatus());
        OrderStatusUpdate response = new OrderStatusUpdate(order.getId(), order.getStatus());
        messagingTemplate.convertAndSend("/topic/orders", response);
        return response;
    }
} 