package com.sporthub.dto.response;

import com.sporthub.enums.BookingStatus;
import com.sporthub.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
@Setter
public class BookingResponse {

    private Long id;

    private Long courtId;

    private LocalDate bookingDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private BigDecimal totalAmount;

    private BookingStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime expiresAt;
    private String bookingCode;
    private BigDecimal unitPrice;
    private PaymentStatus paymentStatus;
    private String cancellationReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private String outcomeReason;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private String customerUsername;
    private String courtName;
    private String venueName;
    private String refundReference;

    public BookingResponse(Long id, Long courtId, LocalDate bookingDate, LocalTime startTime,
            LocalTime endTime, BigDecimal totalAmount, BookingStatus status,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.courtId = courtId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
