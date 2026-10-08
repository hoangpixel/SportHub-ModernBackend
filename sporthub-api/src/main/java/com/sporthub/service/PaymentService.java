package com.sporthub.service;

import com.sporthub.dto.request.CreatePaymentRequest;
import com.sporthub.dto.response.PaymentResponse;
import com.sporthub.entity.Booking;
import com.sporthub.entity.Payment;
import com.sporthub.enums.BookingStatus;
import com.sporthub.enums.PaymentMethod;
import com.sporthub.enums.PaymentStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.BookingRepository;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy booking có id = " + request.getBookingId()));
                        checkBookingOwnership(booking);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException(
                    "Chỉ có thể thanh toán booking đang PENDING");
        }

        if (paymentRepository.existsByBookingId(booking.getId())) {
            throw new ConflictException("Booking này đã được thanh toán");
        }

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(method);
        payment.setTransactionCode("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setAmount(booking.getTotalAmount());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

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