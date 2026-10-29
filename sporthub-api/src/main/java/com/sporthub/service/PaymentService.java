package com.sporthub.service;

import com.sporthub.dto.request.CreatePaymentRequest;
import com.sporthub.dto.response.PaymentResponse;
import com.sporthub.entity.Booking;
import com.sporthub.entity.Court;
import com.sporthub.entity.Payment;
import com.sporthub.enums.BookingStatus;
import com.sporthub.enums.PaymentMethod;
import com.sporthub.enums.PaymentStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.CourtRepository;
import com.sporthub.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder; 

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import com.sporthub.enums.CourtStatus;
import com.sporthub.enums.VenueStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    private final CourtRepository courtRepository;

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Long courtId = bookingRepository.findCourtIdForPayment(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking"));
        Court court = courtRepository.findByIdForBooking(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));
        Booking booking = bookingRepository.findByIdForUpdate(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy booking có id = " + request.getBookingId()));
        checkBookingOwnership(booking);

        if (booking.getStatus() != BookingStatus.PENDING || BookingTime.expired(booking, BookingTime.now())) {
            throw new ConflictException("Chỉ có thể thanh toán booking PENDING còn hạn giữ chỗ");
        }
        if (court.getStatus() != CourtStatus.ACTIVE
                || court.getVenue().getStatus() != VenueStatus.ACTIVE) {
            throw new ConflictException("Sân hoặc cơ sở hiện không hoạt động");
        }

        if (paymentRepository.existsByBookingId(booking.getId())) {
            throw new ConflictException("Booking này đã được thanh toán");
        }

        LocalDateTime startAt = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        if (!startAt.isAfter(BookingTime.now())) {
            throw new ConflictException("Đã quá thời hạn thanh toán trước giờ bắt đầu");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException("Phương thức thanh toán không được để trống");
        }

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(method);
        payment.setTransactionCode("TXN-" + UUID.randomUUID());
        payment.setAmount(booking.getTotalAmount());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(BookingTime.now());

        paymentRepository.save(payment);

        // Cập nhật trạng thái booking
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.saveAndFlush(booking);

        log.info("Payment created: txn={}, bookingId={}", payment.getTransactionCode(), booking.getId());

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thanh toán cho booking id = " + bookingId));
        checkBookingOwnership(payment.getBooking());
        
        return toResponse(payment);
    }

    private void checkBookingOwnership(Booking booking) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Bạn chưa đăng nhập"
            );
        }

        String username = authentication.getName();

        if (!booking.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "Bạn không có quyền truy cập thanh toán này"
            );
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getPaymentMethod(),
                payment.getTransactionCode(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
