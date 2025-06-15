package com.example.paymentservice.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OptimisticLocking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_tasks")
@Data
@OptimisticLocking
public class PaymentTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentTaskStatus status = PaymentTaskStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "error_message")
    private String errorMessage;
} 