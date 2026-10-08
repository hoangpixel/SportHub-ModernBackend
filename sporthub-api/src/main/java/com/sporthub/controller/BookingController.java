package com.sporthub.controller;

import com.sporthub.dto.request.CreateBookingRequest;
import com.sporthub.dto.response.BookingResponse;
import com.sporthub.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_BOOKING')")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('VIEW_OWN_BOOKINGS')")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.getMyBookings(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_OWN_BOOKINGS')")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('VIEW_OWN_BOOKINGS')")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('VIEW_OWNER_BOOKINGS')")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.confirmBooking(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('VIEW_OWNER_BOOKINGS')")
    public ResponseEntity<BookingResponse> completeBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.completeBooking(id));
    }
}