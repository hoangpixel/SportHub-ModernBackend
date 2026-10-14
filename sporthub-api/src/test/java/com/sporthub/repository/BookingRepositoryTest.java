package com.sporthub.repository;

import com.sporthub.entity.Booking;
import com.sporthub.entity.User;
import com.sporthub.enums.BookingStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never"
})
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    private final LocalDate date = LocalDate.of(2030, 6, 10);

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("repository-user");
        user.setEmail("repository-user@example.com");
        user.setPasswordHash("test-password-hash");

        user = userRepository.saveAndFlush(user);
    }

    private void saveBooking(BookingStatus status) {
        Booking booking = new Booking();

        booking.setUser(user);
        booking.setCourtId(1L);
        booking.setBookingCode("BK-" + UUID.randomUUID());
        booking.setBookingDate(date);
        booking.setStartTime(LocalTime.of(8, 0));
        booking.setEndTime(LocalTime.of(10, 0));
        booking.setUnitPrice(new BigDecimal("100000.00"));
        booking.setTotalAmount(new BigDecimal("200000.00"));
        booking.setStatus(status);

        bookingRepository.saveAndFlush(booking);
    }

    private boolean overlaps(
            Long courtId,
            LocalDate bookingDate,
            int startHour,
            int endHour) {

        return bookingRepository.existsOverlapping(
                courtId,
                bookingDate,
                LocalTime.of(startHour, 0),
                LocalTime.of(endHour, 0)
        );
    }

    @Test
    void partialOverlap_returnsTrue() {
        saveBooking(BookingStatus.PENDING);

        assertTrue(overlaps(1L, date, 9, 11));
    }

    @Test
    void sameInterval_returnsTrue() {
        saveBooking(BookingStatus.PENDING);

        assertTrue(overlaps(1L, date, 8, 10));
    }

    @Test
    void containingExistingBooking_returnsTrue() {
        saveBooking(BookingStatus.CONFIRMED);

        assertTrue(overlaps(1L, date, 7, 11));
    }

    @Test
    void adjacentAfter_returnsFalse() {
        saveBooking(BookingStatus.PENDING);

        assertFalse(overlaps(1L, date, 10, 12));
    }

    @Test
    void adjacentBefore_returnsFalse() {
        saveBooking(BookingStatus.PENDING);

        assertFalse(overlaps(1L, date, 6, 8));
    }

    @Test
    void differentCourt_returnsFalse() {
        saveBooking(BookingStatus.PENDING);

        assertFalse(overlaps(2L, date, 8, 10));
    }

    @Test
    void differentDate_returnsFalse() {
        saveBooking(BookingStatus.PENDING);

        assertFalse(overlaps(1L, date.plusDays(1), 8, 10));
    }

    @Test
    void cancelledBooking_doesNotBlockTime() {
        saveBooking(BookingStatus.CANCELLED);

        assertFalse(overlaps(1L, date, 8, 10));
    }
}