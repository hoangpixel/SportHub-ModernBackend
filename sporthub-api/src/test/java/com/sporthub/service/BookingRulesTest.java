package com.sporthub.service;

import com.sporthub.dto.request.CreateBookingRequest;
import com.sporthub.dto.request.CreatePaymentRequest;
import com.sporthub.dto.request.CreateReviewRequest;
import com.sporthub.entity.*;
import com.sporthub.enums.*;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingRulesTest {
    @Mock BookingRepository bookings;
    @Mock UserRepository users;
    @Mock CourtRepository courts;
    @Mock CourtScheduleRepository schedules;
    @Mock PaymentRepository payments;
    @Mock ReviewRepository reviews;
    BookingService bookingService;
    PaymentService paymentService;
    ReviewService reviewService;
    Court court;
    Booking booking;
    User player;
    CreateBookingRequest request;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookings, users, courts, schedules, payments);
        paymentService = new PaymentService(payments, bookings, courts);
        reviewService = new ReviewService(reviews, bookings, users);
        player = new User(); player.setId(1L); player.setUsername("player");
        User owner = new User(); owner.setUsername("owner");
        Venue venue = new Venue(); venue.setOwner(owner);
        court = new Court(); court.setId(1L); court.setVenue(venue);
        court.setPricePerHour(new BigDecimal("150000"));
        court.setOpenTime(LocalTime.of(6, 0)); court.setCloseTime(LocalTime.of(22, 0));
        request = new CreateBookingRequest(); request.setCourtId(1L);
        request.setBookingDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1));
        request.setStartTime(LocalTime.of(8, 0)); request.setEndTime(LocalTime.of(9, 30));
        booking = new Booking(); booking.setId(1L); booking.setUser(player); booking.setCourtId(1L);
        booking.setBookingDate(request.getBookingDate()); booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime()); booking.setTotalAmount(new BigDecimal("225000"));
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(BookingTime.now().plusMinutes(10));
        lenient().when(bookings.findCourtIdForPayment(1L)).thenReturn(Optional.of(1L));
        lenient().when(users.findByUsername("player")).thenReturn(Optional.of(player));
        lenient().when(courts.findByIdForBooking(1L)).thenReturn(Optional.of(court));
        lenient().when(courts.findById(1L)).thenReturn(Optional.of(court));
        lenient().when(bookings.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        authenticate("player");
    }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void chargesCourtPriceAndSnapshotsUnitPrice() {
        assertEquals(0, new BigDecimal("225000").compareTo(bookingService.createBooking(request).getTotalAmount()));
        verify(bookings).save(argThat(saved -> saved.getUnitPrice().compareTo(court.getPricePerHour()) == 0
                && saved.getBookingCode().length() == 39));
    }

    @Test
    void missingCourtIsNotFound() {
        when(courts.findByIdForBooking(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookingService.createBooking(request));
        verify(bookings, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = CourtStatus.class, names = {"INACTIVE", "MAINTENANCE"})
    void inactiveCourtCannotBeBooked(CourtStatus status) {
        court.setStatus(status);
        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void inactiveVenueCannotBeBooked() {
        court.getVenue().setStatus(VenueStatus.INACTIVE);
        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void closedDayOverridesDefaultHours() {
        CourtSchedule schedule = new CourtSchedule(); schedule.setAvailable(false);
        when(schedules.findByCourt_IdAndDayOfWeek(anyLong(), any())).thenReturn(Optional.of(schedule));
        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void dailyHoursOverrideDefaultHours() {
        CourtSchedule schedule = new CourtSchedule(); schedule.setAvailable(true);
        schedule.setOpenTime(LocalTime.of(10, 0)); schedule.setCloseTime(LocalTime.of(20, 0));
        when(schedules.findByCourt_IdAndDayOfWeek(anyLong(), any())).thenReturn(Optional.of(schedule));
        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
        request.setStartTime(LocalTime.of(10, 0)); request.setEndTime(LocalTime.of(20, 0));
        assertDoesNotThrow(() -> bookingService.createBooking(request));
    }

    @Test
    void missingOpeningHoursFailsClosed() {
        court.setOpenTime(null);
        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 22})
    void outsideOpeningHoursCannotBeBooked(int startHour) {
        request.setStartTime(LocalTime.of(startHour, 0)); request.setEndTime(LocalTime.of(startHour + 1, 0));
        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void missingOrNegativePriceCannotBeBooked() {
        court.setPricePerHour(null);
        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
        court.setPricePerHour(BigDecimal.valueOf(-1));
        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
        court.setPricePerHour(BigDecimal.ZERO);
        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void subsecondTimesCannotCollapseWhenPersisted() {
        request.setStartTime(LocalTime.of(8, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(request));
        verify(bookings, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    void supportedDemoPaymentMethodsAreAccepted(PaymentMethod method) {
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(method, paymentService.createPayment(paymentRequest(method.name())).getPaymentMethod());
    }

    @Test
    void anotherOwnerCannotCompleteOwnedBooking() {
        authenticate("another-owner", "ROLE_OWNER");
        booking.setStatus(BookingStatus.CONFIRMED);
        assertThrows(AccessDeniedException.class, () -> bookingService.completeBooking(1L));
        verify(bookings, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"CONFIRMED", "CANCELLED", "COMPLETED", "EXPIRED", "NO_SHOW"})
    void cannotCancelNonPendingBooking(BookingStatus status) {
        booking.setStatus(status);
        assertThrows(ConflictException.class, () -> bookingService.cancelBooking(1L));
    }

    @Test
    void cannotCancelPendingBookingWithPayment() {
        when(payments.existsByBookingId(1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> bookingService.cancelBooking(1L));
    }

    @Test
    void anotherPlayerCannotCancel() {
        authenticate("another-player");
        assertThrows(AccessDeniedException.class, () -> bookingService.cancelBooking(1L));
    }

    @Test
    void pastBookingCannotBeCancelledOrPaid() {
        booking.setBookingDate(request.getBookingDate().minusDays(2));
        authenticate("owner", "ROLE_OWNER");
        authenticate("player");
        assertThrows(ConflictException.class, () -> bookingService.cancelBooking(1L));
        assertThrows(ConflictException.class, () -> paymentService.createPayment(paymentRequest("CASH")));
    }

    @Test
    void completeRequiresElapsedTimeAndCompletedPayment() {
        booking.setStatus(BookingStatus.CONFIRMED); authenticate("owner", "ROLE_OWNER");
        assertThrows(ConflictException.class, () -> bookingService.completeBooking(1L));
        booking.setBookingDate(request.getBookingDate().minusDays(2));
        assertThrows(ConflictException.class, () -> bookingService.completeBooking(1L));
        when(payments.existsByBookingIdAndStatus(1L, PaymentStatus.COMPLETED)).thenReturn(true);
        assertEquals(BookingStatus.COMPLETED, bookingService.completeBooking(1L).getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"PENDING", "CANCELLED", "COMPLETED", "EXPIRED", "NO_SHOW"})
    void completeRejectsInvalidSourceStatus(BookingStatus status) {
        booking.setStatus(status); authenticate("owner", "ROLE_OWNER");
        assertThrows(ConflictException.class, () -> bookingService.completeBooking(1L));
    }

    @Test
    void confirmedBookingCannotBePaidWithoutPendingHold() {
        booking.setStatus(BookingStatus.CONFIRMED);
        assertThrows(ConflictException.class, () -> paymentService.createPayment(paymentRequest(" cash ")));
        verify(payments, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"CANCELLED", "COMPLETED", "EXPIRED", "NO_SHOW"})
    void cannotPayTerminalBooking(BookingStatus status) {
        booking.setStatus(status);
        assertThrows(ConflictException.class, () -> paymentService.createPayment(paymentRequest("CASH")));
    }

    @Test
    void duplicatePaymentIsConflict() {
        when(payments.existsByBookingId(1L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> paymentService.createPayment(paymentRequest("CASH")));
        verify(payments, never()).save(any());
    }

    @Test
    void invalidPaymentMethodsDoNotWrite() {
        for (String method : new String[] {null, "", "  ", "unsupported"}) {
            assertThrows(IllegalArgumentException.class, () -> paymentService.createPayment(paymentRequest(method)));
        }
        verify(payments, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"PENDING", "CONFIRMED", "CANCELLED", "EXPIRED", "NO_SHOW"})
    void reviewsRequireCompletedBooking(BookingStatus status) {
        booking.setStatus(status);
        assertThrows(IllegalArgumentException.class, () -> reviewService.createReview(reviewRequest()));
        verify(reviews, never()).save(any());
    }

    @Test
    void anotherPlayerCannotReview() {
        User anotherPlayer = new User(); anotherPlayer.setId(2L); anotherPlayer.setUsername("another-player");
        when(users.findByUsername("another-player")).thenReturn(Optional.of(anotherPlayer));
        authenticate("another-player");
        assertThrows(AccessDeniedException.class, () -> reviewService.createReview(reviewRequest()));
    }

    @Test
    void invalidPaginationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> bookingService.getMyBookings(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> bookingService.getMyBookings(0, 101));
        assertThrows(IllegalArgumentException.class, () -> reviewService.getReviewsByCourtId(1L, 0, 0));
    }

    private CreatePaymentRequest paymentRequest(String method) {
        CreatePaymentRequest result = new CreatePaymentRequest(); result.setBookingId(1L); result.setPaymentMethod(method);
        return result;
    }

    private CreateReviewRequest reviewRequest() {
        CreateReviewRequest result = new CreateReviewRequest(); result.setBookingId(1L); result.setRating(5);
        return result;
    }

    private void authenticate(String username, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
