package com.sporthub.dto.response;

import com.sporthub.enums.PaymentMethod;
import com.sporthub.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private PaymentMethod paymentMethod;
    private String transactionCode;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}