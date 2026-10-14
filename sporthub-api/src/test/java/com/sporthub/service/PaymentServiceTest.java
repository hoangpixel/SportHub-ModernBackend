package com.sporthub.service;

import com.sporthub.dto.request.CreatePaymentRequest;
import com.sporthub.dto.response.PaymentResponse;
import com.sporthub.entity.Booking;
import com.sporthub.entity.Payment;
import com.sporthub.enums.BookingStatus;
import com.sporthub.enums.PaymentStatus;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.PaymentRepository;
import com.sporthub.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(BigDecimal.valueOf(200000));
        User user = new User();
        user.setId(1L);
        user.setUsername("user01");

        booking.setUser(user);

        var context = SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user01",
                        null,
                        List.of()
                )
        );

        SecurityContextHolder.setContext(context);
    }

    @Test
    void createPayment_success() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBookingId(1L);
        request.setPaymentMethod("VNPAY");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(
                0,
                BigDecimal.valueOf(200000).compareTo(response.getAmount())
        );
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPayment_notFoundBooking_shouldThrowException() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBookingId(999L);
        request.setPaymentMethod("VNPAY");

        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_otherUser_shouldThrowAccessDenied() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBookingId(1L);
        request.setPaymentMethod("VNPAY");

        booking.getUser().setUsername("other-user");

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.createPayment(request)
        );

        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void getPayment_otherUser_shouldThrowAccessDenied() {
        booking.getUser().setUsername("other-user");

        Payment payment = new Payment();
        payment.setBooking(booking);

        when(paymentRepository.findByBookingId(1L))
                .thenReturn(Optional.of(payment));

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getPaymentByBookingId(1L)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

}
