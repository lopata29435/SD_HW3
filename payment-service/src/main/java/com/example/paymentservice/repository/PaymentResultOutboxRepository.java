package com.example.paymentservice.repository;

import com.example.paymentservice.model.PaymentResultOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentResultOutboxRepository extends JpaRepository<PaymentResultOutbox, UUID> {
    @Query("SELECT p FROM PaymentResultOutbox p WHERE p.processed = false ORDER BY p.createdAt")
    List<PaymentResultOutbox> findUnprocessedMessages();

    List<PaymentResultOutbox> findByProcessedFalse();
} 