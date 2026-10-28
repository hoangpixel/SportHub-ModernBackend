package com.sporthub.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record CourtAvailabilityResponse(Long courtId, LocalDate date, LocalDateTime calculatedAt,
        boolean acceptingBookings, String reason, List<TimeRange> occupied, List<TimeRange> available) {
    public record TimeRange(LocalTime startTime, LocalTime endTime) {
    }
}
