package com.sporthub.repository;

import com.sporthub.entity.Payment;
import com.sporthub.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    Optional<Payment> findByTransactionCode(String transactionCode);
}
