package com.sporthub.dto.response;

import com.sporthub.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
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
}