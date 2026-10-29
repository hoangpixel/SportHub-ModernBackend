package com.sporthub.service;

import com.sporthub.entity.Booking;
import com.sporthub.enums.BookingStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class BookingTime {
    private BookingTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public static boolean expired(Booking booking, LocalDateTime now) {
        return booking.getStatus() == BookingStatus.PENDING
                && (booking.getExpiresAt() == null || !booking.getExpiresAt().isAfter(now)
                || !LocalDateTime.of(booking.getBookingDate(), booking.getStartTime()).isAfter(now));
    }
}
