package com.example.orderservice.repository;

import com.example.orderservice.model.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {
    @Query("SELECT po FROM PaymentOutbox po WHERE po.processed = false ORDER BY po.createdAt ASC")
    List<PaymentOutbox> findUnprocessedMessages();
} 