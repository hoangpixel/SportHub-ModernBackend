package com.sporthub.controller;

import com.sporthub.dto.request.BookingReasonRequest;
import com.sporthub.dto.request.RecordRefundRequest;
import com.sporthub.dto.response.BookingResponse;
import com.sporthub.enums.BookingStatus;
import com.sporthub.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/owner/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VIEW_OWNER_BOOKINGS')")
public class OwnerBookingController {
    private final BookingService bookings;

    @GetMapping
    public Page<BookingResponse> list(@RequestParam(required = false) Long courtId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return bookings.getManagedBookings(false, courtId, date, status, page, size);
    }

    @GetMapping("/{id}")
    public BookingResponse detail(@PathVariable Long id) {
        return bookings.getManagedBooking(id, false);
    }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, @Valid @RequestBody BookingReasonRequest request) {
        return bookings.cancelManagedBooking(id, request.getReason());
    }

    @PatchMapping("/{id}/complete")
    public BookingResponse complete(@PathVariable Long id) {
        return bookings.completeBooking(id);
    }

    @PatchMapping("/{id}/no-show")
    public BookingResponse noShow(@PathVariable Long id, @Valid @RequestBody BookingReasonRequest request) {
        return bookings.markNoShow(id, request.getReason());
    }
}
