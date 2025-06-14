package com.example.orderservice.repository;

import com.example.orderservice.model.PaymentResultOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentResultOutboxRepository extends JpaRepository<PaymentResultOutbox, UUID> {
    List<PaymentResultOutbox> findByProcessedFalse();
} 