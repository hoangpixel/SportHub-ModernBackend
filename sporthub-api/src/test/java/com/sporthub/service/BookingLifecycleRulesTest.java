package com.sporthub.service;

import com.sporthub.dto.request.CreateBookingRequest;
import com.sporthub.dto.request.CreatePaymentRequest;
import com.sporthub.entity.Booking;
import com.sporthub.entity.Court;
import com.sporthub.entity.CourtSchedule;
import com.sporthub.entity.User;
import com.sporthub.entity.Venue;
import com.sporthub.enums.BookingStatus;
import com.sporthub.enums.CourtStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.CourtRepository;
import com.sporthub.repository.CourtScheduleRepository;
import com.sporthub.repository.PaymentRepository;
import com.sporthub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingLifecycleRulesTest {
    @Mock BookingRepository bookings;
    @Mock CourtRepository courts;
    @Mock CourtScheduleRepository schedules;
    @Mock PaymentRepository payments;
    @Mock UserRepository users;
    final LocalDateTime now = LocalDateTime.of(2030, 1, 1, 12, 0);
    Court court;
    User player;
    BookingService bookingService;
    MockedStatic<BookingTime> time;

    @BeforeEach
    void setUp() {
        time = mockStatic(BookingTime.class, CALLS_REAL_METHODS);
        time.when(BookingTime::now).thenReturn(now);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("player", null, List.of()));
        player = new User();
        player.setUsername("player");
        court = new Court();
        court.setId(1L);
        court.setVenue(new Venue());
        court.setPricePerHour(new BigDecimal("150000"));
        court.setOpenTime(LocalTime.of(6, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        bookingService = new BookingService(bookings, users, courts, schedules, payments);
    }

    @AfterEach
    void cleanUp() {
        time.close();
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 60})
    void holdEndsAtEarlierOfTenMinutesOrStart(int minutesBeforeStart) {
        when(users.findByUsername("player")).thenReturn(Optional.of(player));
        when(courts.findByIdForBooking(1L)).thenReturn(Optional.of(court));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCourtId(1L);
        request.setBookingDate(now.toLocalDate());
        request.setStartTime(now.plusMinutes(minutesBeforeStart).toLocalTime());
        request.setEndTime(request.getStartTime().plusHours(1));
        assertThat(bookingService.createBooking(request).getExpiresAt())
                .isEqualTo(now.plusMinutes(Math.min(10, minutesBeforeStart)));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void expirationBoundaryIsExclusive(int secondsAfterNow) {
        Booking booking = pending();
        booking.setExpiresAt(now.plusSeconds(secondsAfterNow));
        assertThat(BookingTime.expired(booking, now)).isEqualTo(secondsAfterNow <= 0);
    }

    @Test
    void missingDeadlineFailsClosedAndCannotBePaid() {
        Booking booking = pending();
        booking.setExpiresAt(null);
        stubPaymentTarget(booking);
        assertThatThrownBy(() -> new PaymentService(payments, bookings, courts).createPayment(payment()))
                .isInstanceOf(ConflictException.class);
        verify(payments, never()).save(any());
    }

    @Test
    void courtClosedAfterBookingCannotBePaid() {
        Booking booking = pending();
        stubPaymentTarget(booking);
        court.setStatus(CourtStatus.MAINTENANCE);
        assertThatThrownBy(() -> new PaymentService(payments, bookings, courts).createPayment(payment()))
                .isInstanceOf(ConflictException.class);
        verify(payments, never()).save(any());
    }

    @Test
    void availabilityUsesWeeklyScheduleAndMergesOccupiedIntervals() {
        when(courts.findById(1L)).thenReturn(Optional.of(court));
        CourtSchedule schedule = new CourtSchedule();
        schedule.setAvailable(true);
        schedule.setOpenTime(LocalTime.of(9, 0));
        schedule.setCloseTime(LocalTime.of(12, 0));
        when(schedules.findByCourt_IdAndDayOfWeek(eq(1L), any())).thenReturn(Optional.of(schedule));
        Booking first = interval(9, 10);
        Booking second = interval(10, 11);
        when(bookings.findOccupying(eq(1L), any(), eq(now))).thenReturn(List.of(first, second));
        var result = new CourtAvailabilityService(courts, schedules, bookings).get(1L, now.toLocalDate().plusDays(1));
        assertThat(result.available()).hasSize(1);
        assertThat(result.available().getFirst().startTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(result.available().getFirst().endTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void todayAvailabilityNeverOffersPastOrCurrentSecond() {
        when(courts.findById(1L)).thenReturn(Optional.of(court));
        var result = new CourtAvailabilityService(courts, schedules, bookings).get(1L, now.toLocalDate());
        assertThat(result.available()).hasSize(1);
        assertThat(result.available().getFirst().startTime()).isEqualTo(LocalTime.of(12, 0, 1));
    }

    @Test
    void weeklyClosedDayReturnsNoAvailability() {
        when(courts.findById(1L)).thenReturn(Optional.of(court));
        CourtSchedule schedule = new CourtSchedule();
        schedule.setAvailable(false);
        when(schedules.findByCourt_IdAndDayOfWeek(eq(1L), any())).thenReturn(Optional.of(schedule));
        var result = new CourtAvailabilityService(courts, schedules, bookings).get(1L, now.toLocalDate().plusDays(1));
        assertThat(result.acceptingBookings()).isFalse();
        assertThat(result.available()).isEmpty();
        verifyNoInteractions(bookings);
    }

    private Booking pending() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setCourtId(1L);
        booking.setUser(player);
        booking.setBookingDate(now.toLocalDate().plusDays(1));
        booking.setStartTime(LocalTime.of(14, 0));
        booking.setEndTime(LocalTime.of(15, 0));
        booking.setExpiresAt(now.plusMinutes(10));
        return booking;
    }

    private Booking interval(int startHour, int endHour) {
        Booking booking = new Booking();
        booking.setStartTime(LocalTime.of(startHour, 0));
        booking.setEndTime(LocalTime.of(endHour, 0));
        return booking;
    }

    private void stubPaymentTarget(Booking booking) {
        when(bookings.findCourtIdForPayment(1L)).thenReturn(Optional.of(1L));
        when(courts.findByIdForBooking(1L)).thenReturn(Optional.of(court));
        when(bookings.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
    }

    private CreatePaymentRequest payment() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBookingId(1L);
        request.setPaymentMethod("CASH");
        return request;
    }
}
