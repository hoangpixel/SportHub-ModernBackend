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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    // =========================
    // TẠO BOOKING
    // =========================
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        // 1. Lấy current user
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // 2. Validate startTime < endTime
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
        }

        // 3. Validate bookingDate không phải quá khứ
        LocalDateTime startAt = LocalDateTime.of(
                request.getBookingDate(),
                request.getStartTime()
        );

        LocalDateTime now =
                LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        if (!startAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Thời gian bắt đầu phải ở tương lai"
            );
        }

        // 4. Kiểm tra overlap (business rule quan trọng nhất)
        boolean overlapping = bookingRepository.existsOverlapping(
                request.getCourtId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime()
        );
        if (overlapping) {
            throw new ConflictException("Sân đã có người đặt trong khung giờ này");
        }

        // 5. Tính tiền (tính tạm thời, sau này có thể thay đổi theo court)
        BigDecimal pricePerHour = new BigDecimal("100000");
        long seconds = Duration.between(
                request.getStartTime(),
                request.getEndTime()
        ).getSeconds();

        BigDecimal totalAmount = pricePerHour
                .multiply(BigDecimal.valueOf(seconds))
                .divide(
                        BigDecimal.valueOf(3600),
                        2,
                        RoundingMode.HALF_UP
                );

        // 6. Tạo booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourtId(request.getCourtId());
        booking.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setUnitPrice(pricePerHour);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.PENDING);

        bookingRepository.save(booking);
        log.info("Booking created: code={}, user={}", booking.getBookingCode(), username);

        return toResponse(booking);
    }

    // =========================
    // XEM BOOKING CỦA TÔI
    // =========================
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(int page, int size) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return bookingRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    // =========================
    // XEM CHI TIẾT BOOKING
    // =========================
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = findBookingById(id);
        checkOwnership(booking);
        return toResponse(booking);
    }

    // =========================
    // HỦY BOOKING
    // =========================
    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = findBookingById(id);
        checkOwnership(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking đã bị hủy trước đó");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Không thể hủy booking đã hoàn tất");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(booking);

        log.info("Booking cancelled: id={}", id);
        return toResponse(booking);
    }

    // =========================
    // CONFIRM BOOKING (Owner/Admin)
    // =========================
    @Transactional
    public BookingResponse confirmBooking(Long id) {
        Booking booking = findBookingById(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể xác nhận booking đang PENDING");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.saveAndFlush(booking);

        return toResponse(booking);
    }

    // =========================
    // COMPLETE BOOKING
    // =========================
    @Transactional
    public BookingResponse completeBooking(Long id) {
        Booking booking = findBookingById(id);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Chỉ có thể hoàn tất booking đã CONFIRMED");
        }

        LocalDateTime endAt = LocalDateTime.of(
                booking.getBookingDate(),
                booking.getEndTime()
        );

        LocalDateTime now =
                LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        if (now.isBefore(endAt)) {
            throw new ConflictException(
                    "Chưa đến thời điểm kết thúc booking"
            );
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.saveAndFlush(booking);

        return toResponse(booking);
    }

    // =========================
    // HELPERS
    // =========================
    private Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking có id = " + id));
    }

    private void checkOwnership(Booking booking) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        if (!booking.getUser().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền truy cập booking này");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCourtId(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}