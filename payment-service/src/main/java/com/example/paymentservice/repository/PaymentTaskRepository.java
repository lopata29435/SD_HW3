package com.example.paymentservice.repository;

import com.example.paymentservice.model.PaymentTask;
import com.example.paymentservice.model.PaymentTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTaskRepository extends JpaRepository<PaymentTask, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTask pt WHERE pt.id = :id")
    Optional<PaymentTask> findByIdWithLock(@Param("id") UUID id);

    List<PaymentTask> findByOrderIdAndStatus(UUID orderId, PaymentTaskStatus status);
} 