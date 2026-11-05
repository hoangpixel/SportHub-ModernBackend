package com.sporthub.seeder;

import com.sporthub.entity.Booking;
import com.sporthub.entity.Court;
import com.sporthub.entity.Payment;
import com.sporthub.entity.Review;
import com.sporthub.entity.User;
import com.sporthub.entity.Venue;
import com.sporthub.enums.BookingStatus;
import com.sporthub.enums.CourtStatus;
import com.sporthub.enums.PaymentMethod;
import com.sporthub.enums.PaymentStatus;
import com.sporthub.enums.ReviewStatus;
import com.sporthub.enums.VenueStatus;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.CourtRepository;
import com.sporthub.repository.PaymentRepository;
import com.sporthub.repository.ReviewRepository;
import com.sporthub.repository.UserRepository;
import com.sporthub.repository.VenueRepository;
import com.sporthub.service.BookingTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class BookingDemoSeeder {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void seed() {
        User user01 = requireUser("user01");
        User user02 = requireUser("user02");
        User user03 = requireUser("user03");
        User user04 = requireUser("user04");
        User user05 = requireUser("user05");
        User user06 = requireUser("user06");
        User user07 = requireUser("user07");
        User owner01 = requireUser("owner01");
        User owner02 = requireUser("owner02");

        Venue arena = requireVenue("SportHub Arena", owner01);
        Venue green = requireVenue("Green Court Center", owner01);
        Venue club = requireVenue("Saigon Badminton Club", owner02);
        Venue park = requireVenue("Riverside Football Park", owner01);
        Venue hub = requireVenue("Thao Dien Pickleball Hub", owner02);
        Venue volleyball = requireVenue("City Volleyball Center", owner01);

        Court footballA = requireCourt(arena, "Sân bóng đá 5 người - A");
        Court footballB = requireCourt(arena, "Sân bóng đá 5 người - B");
        Court badminton1 = requireCourt(arena, "Sân cầu lông 1");
        Court basketball = requireCourt(arena, "Sân bóng rổ trong nhà");
        Court tennisA = requireCourt(green, "Sân tennis A");
        Court tennisB = requireCourt(green, "Sân tennis B");
        Court pickleballA = requireCourt(green, "Sân pickleball A");
        Court badminton2 = requireCourt(club, "Sân cầu lông 2");
        Court badminton3 = requireCourt(club, "Sân cầu lông 3");
        Court footballC = requireCourt(park, "Sân bóng đá 5 người - C");
        Court pickleballB = requireCourt(hub, "Sân pickleball B");
        Court volleyball1 = requireCourt(volleyball, "Sân bóng chuyền 1");

        LocalDate today = LocalDate.now(ZONE);
        // Giu nguyen ba booking cu de khong anh huong minh chung da chup.
        seedBooking("DEMO-BK-CONFIRMED", user01, footballA, today.plusDays(2), 18, 19, BookingStatus.CONFIRMED, 0);
        seedBooking("DEMO-BK-COMPLETED", user01, badminton1, today.minusDays(2), 8, 9, BookingStatus.COMPLETED, 5);
        seedBooking("DEMO-BK-CANCELLED", user01, tennisA, today.plusDays(3), 10, 11, BookingStatus.CANCELLED, 0);

        // Chinh sach: 10 completed (co review), 3 confirmed, 2 cancelled, 1 no-show, 1 expired.
        seedBooking("DEMO-BK-004", user02, footballB, today.minusDays(12), 18, 19, BookingStatus.COMPLETED, 4);
        seedBooking("DEMO-BK-005", user03, badminton2, today.minusDays(11), 7, 8, BookingStatus.COMPLETED, 5);
        seedBooking("DEMO-BK-006", user04, tennisB, today.minusDays(10), 17, 18, BookingStatus.COMPLETED, 4);
        seedBooking("DEMO-BK-007", user05, pickleballA, today.minusDays(9), 9, 10, BookingStatus.COMPLETED, 5);
        seedBooking("DEMO-BK-008", user06, badminton3, today.minusDays(8), 19, 20, BookingStatus.COMPLETED, 3);
        seedBooking("DEMO-BK-009", user07, basketball, today.minusDays(7), 8, 9, BookingStatus.COMPLETED, 5);
        seedBooking("DEMO-BK-010", owner02, footballC, today.minusDays(6), 16, 17, BookingStatus.COMPLETED, 4);
        seedBooking("DEMO-BK-011", owner01, pickleballB, today.minusDays(5), 18, 19, BookingStatus.COMPLETED, 5);
        seedBooking("DEMO-BK-012", user02, volleyball1, today.minusDays(4), 17, 18, BookingStatus.COMPLETED, 4);
        seedBooking("DEMO-BK-013", user03, badminton2, today.plusDays(4), 8, 9, BookingStatus.CONFIRMED, 0);
        seedBooking("DEMO-BK-014", user04, tennisB, today.plusDays(5), 19, 20, BookingStatus.CONFIRMED, 0);
        seedBooking("DEMO-BK-015", user05, pickleballA, today.plusDays(6), 15, 16, BookingStatus.CANCELLED, 0);
        seedBooking("DEMO-BK-016", user06, footballB, today.minusDays(3), 17, 18, BookingStatus.NO_SHOW, 0);
        seedBooking("DEMO-BK-017", user07, tennisA, today.plusDays(7), 11, 12, BookingStatus.EXPIRED, 0);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Thieu tai khoan demo: " + username));
    }

    private Venue requireVenue(String name, User owner) {
        return venueRepository.findByNameIgnoreCaseAndOwner_Id(name, owner.getId())
                .orElseThrow(() -> new IllegalStateException("Thieu venue demo: " + name));
    }

    private Court requireCourt(Venue venue, String name) {
        return courtRepository.findByNameIgnoreCaseAndVenue_Id(name, venue.getId())
                .orElseThrow(() -> new IllegalStateException("Thieu court demo: " + name));
    }

    private void seedBooking(String code, User user, Court court, LocalDate date,
                             int startHour, int endHour, BookingStatus status, int rating) {
        LocalTime start = LocalTime.of(startHour, 0);
        LocalTime end = LocalTime.of(endHour, 0);
        Booking booking = bookingRepository.findByBookingCode(code).orElse(null);

        if (booking == null) {
            if (court.getStatus() != CourtStatus.ACTIVE || court.getVenue().getStatus() != VenueStatus.ACTIVE) {
                return;
            }
            // Khong chen lich trung voi booking da co; booking huy/het han khong chiem cho.
            if (status != BookingStatus.CANCELLED && status != BookingStatus.EXPIRED
                    && bookingRepository.existsActiveOverlap(court.getId(), date, start, end, BookingTime.now())) {
                return;
            }
            if (court.getPricePerHour() == null || court.getPricePerHour().signum() <= 0) {
                throw new IllegalStateException("Don gia san khong hop le: " + court.getName());
            }

            BigDecimal amount = court.getPricePerHour()
                    .multiply(BigDecimal.valueOf(Duration.between(start, end).toSeconds()))
                    .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

            booking = new Booking();
            booking.setBookingCode(code);
            booking.setUser(user);
            booking.setCourtId(court.getId());
            booking.setBookingDate(date);
            booking.setStartTime(start);
            booking.setEndTime(end);
            booking.setUnitPrice(court.getPricePerHour());
            booking.setTotalAmount(amount);
            booking.setStatus(status);
            booking.setNote("Dữ liệu demo SportHub - không phải đặt sân thực tế");

            if (status == BookingStatus.CANCELLED) {
                booking.setCancellationReason("Người dùng hủy Booking demo");
                booking.setCancelledAt(BookingTime.now());
                booking.setCancelledBy(user.getUsername());
            } else if (status == BookingStatus.COMPLETED || status == BookingStatus.NO_SHOW) {
                booking.setResolvedBy("owner01");
                booking.setResolvedAt(LocalDateTime.of(date, end).plusMinutes(5));
                if (status == BookingStatus.NO_SHOW) {
                    booking.setOutcomeReason("Khách không đến sân (dữ liệu demo)");
                }
            } else if (status == BookingStatus.EXPIRED) {
                booking.setExpiresAt(BookingTime.now().minusMinutes(10));
            }
            booking = bookingRepository.saveAndFlush(booking);
        }

        // Phuc hoi payment/review thieu, nhung khong ghi de trang thai booking da co.
        BookingStatus currentStatus = booking.getStatus();
        if ((currentStatus == BookingStatus.CONFIRMED || currentStatus == BookingStatus.COMPLETED
                || currentStatus == BookingStatus.NO_SHOW) && !paymentRepository.existsByBookingId(booking.getId())) {
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setPaymentMethod(paymentMethodFor(code));
            payment.setTransactionCode(code.replace("DEMO-BK", "DEMO-TXN"));
            payment.setAmount(booking.getTotalAmount());
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(currentStatus == BookingStatus.CONFIRMED
                    ? BookingTime.now() : LocalDateTime.of(booking.getBookingDate(), booking.getStartTime()).minusDays(1));
            paymentRepository.save(payment);
        }
        if (currentStatus == BookingStatus.COMPLETED && rating >= 1
                && !reviewRepository.existsByBookingId(booking.getId())) {
            Review review = new Review();
            review.setBooking(booking);
            review.setUser(booking.getUser());
            review.setCourtId(booking.getCourtId());
            review.setRating(rating);
            review.setComment("Trải nghiệm tại " + court.getName() + " tốt, phục vụ chu đáo. (Dữ liệu demo)");
            review.setStatus(ReviewStatus.VISIBLE);
            reviewRepository.save(review);
        }
    }

    private PaymentMethod paymentMethodFor(String code) {
        return switch (Math.floorMod(code.hashCode(), 3)) {
            case 0 -> PaymentMethod.BANK_TRANSFER;
            case 1 -> PaymentMethod.MOMO;
            default -> PaymentMethod.CASH;
        };
    }
}
