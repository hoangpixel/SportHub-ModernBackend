package com.sporthub.service;

import com.sporthub.dto.request.CreateBookingRequest;
import com.sporthub.dto.response.BookingResponse;
import com.sporthub.entity.Booking;
import com.sporthub.entity.User;
import com.sporthub.enums.BookingStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, userRepository);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("user01");
    }

    // TẠO BOOKING THÀNH CÔNG
    @Test
    void createBooking_success() {
        // Arrange
        CreateBookingRequest request = createRequest();
        User user = createUser();

        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));
        when(bookingRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getCourtId());
        assertEquals(BookingStatus.PENDING, response.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }

    // OVERLAP: SÂN ĐÃ CÓ NGƯỜI ĐẶT
    @Test
    void createBooking_overlap_shouldThrowConflict() {
        // Arrange
        CreateBookingRequest request = createRequest();
        User user = createUser();

        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));
        when(bookingRepository.existsOverlapping(
                request.getCourtId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime()
        )).thenReturn(true);

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class,
                () -> bookingService.createBooking(request));

        assertEquals("Sân đã có người đặt trong khung giờ này", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    // GIỜ BẮT ĐẦU SAU GIỜ KẾT THÚC
    @Test
    void createBooking_invalidTime_shouldThrowException() {
        CreateBookingRequest request = createRequest();
        request.setStartTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(16, 0));

        User user = createUser();
        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(request));
    }

    // NGÀY TRONG QUÁ KHỨ
    @Test
    void createBooking_pastDate_shouldThrowException() {
        CreateBookingRequest request = createRequest();
        request.setBookingDate(LocalDate.of(2020, 1, 1));

        User user = createUser();
        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(request));
    }

    // HỦY BOOKING THÀNH CÔNG
    @Test
    void cancelBooking_success() {
        Booking booking = createBooking(BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.saveAndFlush(any())).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking(1L);

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
    }

    // HỦY BOOKING ĐÃ HOÀN TẤT
    @Test
    void cancelBooking_alreadyCompleted_shouldThrowException() {
        Booking booking = createBooking(BookingStatus.COMPLETED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.cancelBooking(1L));
    }

    // BOOKING KHÔNG TỒN TẠI
    @Test
    void getBookingById_notFound_shouldThrowException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.getBookingById(999L));
    }

    // HELPERS
    private CreateBookingRequest createRequest() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourtId(1L);
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(10, 0));
        return request;
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user01");
        return user;
    }

    private Booking createBooking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(1L);
        User user = createUser();
        booking.setUser(user);
        booking.setCourtId(1L);
        booking.setBookingCode("BK-TEST0001");
        booking.setBookingDate(LocalDate.now().plusDays(1));
        booking.setStartTime(LocalTime.of(8, 0));
        booking.setEndTime(LocalTime.of(10, 0));
        booking.setStatus(status);
        return booking;
    }

    @Test
    void createBooking_90Minutes_shouldChargeOneAndHalfHours() {
        CreateBookingRequest request = createRequest();
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 30));

        when(userRepository.findByUsername("user01"))
                .thenReturn(Optional.of(createUser()));

        when(bookingRepository.existsOverlapping(
                request.getCourtId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime()
        )).thenReturn(false);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking = invocation.getArgument(0);
                    booking.setId(1L);
                    return booking;
                });

        BookingResponse response = bookingService.createBooking(request);

        assertEquals(
                0,
                new BigDecimal("150000.00")
                        .compareTo(response.getTotalAmount())
        );
    }

    @Test
    void getBookingById_otherUser_shouldThrowAccessDenied() {
        Booking booking = createBooking(BookingStatus.PENDING);
        booking.getUser().setUsername("other-user");

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> bookingService.getBookingById(1L)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
