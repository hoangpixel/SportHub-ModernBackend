package com.sporthub.service;

import com.sporthub.dto.request.CreateBookingRequest;
import com.sporthub.dto.response.BookingResponse;
import com.sporthub.entity.Booking;
import com.sporthub.entity.Court;
import com.sporthub.entity.CourtSchedule;
import com.sporthub.entity.User;
import com.sporthub.enums.BookingStatus;
import com.sporthub.enums.CourtStatus;
import com.sporthub.enums.PaymentStatus;
import com.sporthub.enums.VenueStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.CourtRepository;
import com.sporthub.repository.CourtScheduleRepository;
import com.sporthub.repository.PaymentRepository;
import com.sporthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final CourtScheduleRepository courtScheduleRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse createBooking(CreateBookingRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
        }
        if (request.getStartTime().getNano() != 0 || request.getEndTime().getNano() != 0) {
            throw new IllegalArgumentException("Giờ đặt sân chỉ hỗ trợ độ chính xác đến giây");
        }

        Court court = courtRepository.findByIdForBooking(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));
        requireFutureStart(LocalDateTime.of(request.getBookingDate(), request.getStartTime()));
        if (court.getStatus() != CourtStatus.ACTIVE || court.getVenue().getStatus() != VenueStatus.ACTIVE) {
            throw new ConflictException("Sân hoặc cơ sở hiện không hoạt động");
        }
        validateOpeningHours(court, request);
        if (bookingRepository.existsActiveOverlap(court.getId(), request.getBookingDate(),
                request.getStartTime(), request.getEndTime(), BookingTime.now())) {
            throw new ConflictException("Sân đã có người đặt trong khung giờ này");
        }
        BigDecimal pricePerHour = court.getPricePerHour();
        if (pricePerHour == null || pricePerHour.signum() <= 0) {
            throw new ConflictException("Sân chưa có đơn giá hợp lệ");
        }
        long seconds = Duration.between(request.getStartTime(), request.getEndTime()).getSeconds();
        BigDecimal totalAmount = pricePerHour.multiply(BigDecimal.valueOf(seconds))
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourtId(court.getId());
        booking.setBookingCode("BK-" + UUID.randomUUID());
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setUnitPrice(pricePerHour);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.PENDING);
        LocalDateTime deadline = BookingTime.now().plusMinutes(10);
        LocalDateTime startAt = LocalDateTime.of(request.getBookingDate(), request.getStartTime());
        booking.setExpiresAt(deadline.isBefore(startAt) ? deadline : startAt);
        bookingRepository.save(booking);
        log.info("Booking created: code={}, user={}", booking.getBookingCode(), username);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        Pageable pageable = SearchPage.of(page, size, "createdAt", "desc", Set.of("createdAt"));
        return bookingRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking có id = " + id));
        checkOwnership(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = findBookingForUpdate(id);
        checkOwnership(booking);
        requireLiveHold(booking);
        if (booking.getStatus() != BookingStatus.PENDING || paymentRepository.existsByBookingId(id)) {
            throw new ConflictException("Chỉ có thể hủy booking PENDING chưa thanh toán");
        }
        requireFutureStart(LocalDateTime.of(booking.getBookingDate(), booking.getStartTime()));
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason("Customer cancelled before payment");
        booking.setCancelledAt(BookingTime.now());
        booking.setCancelledBy(SecurityContextHolder.getContext().getAuthentication().getName());
        bookingRepository.saveAndFlush(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse completeBooking(Long id) {
        Booking booking = findBookingForUpdate(id);
        checkCourtManagement(booking);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Chỉ có thể hoàn tất booking đã CONFIRMED");
        }
        LocalDateTime endAt = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());
        if (BookingTime.now().isBefore(endAt)) {
            throw new ConflictException("Chưa đến thời điểm kết thúc booking");
        }
        if (!paymentRepository.existsByBookingIdAndStatus(id, PaymentStatus.COMPLETED)) {
            throw new ConflictException("Booking chưa có thanh toán hoàn tất");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setResolvedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        booking.setResolvedAt(BookingTime.now());
        bookingRepository.saveAndFlush(booking);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getManagedBookings(boolean admin, Long courtId, java.time.LocalDate date,
            BookingStatus status, int page, int size) {
        String owner = admin ? null : SecurityContextHolder.getContext().getAuthentication().getName();
        if (admin) {
            requireAdmin();
        }
        Pageable pageable = SearchPage.of(page, size, "createdAt", "desc", Set.of("createdAt"));
        return bookingRepository.findManaged(owner, courtId, date, status == null ? null : status.name(),
                BookingTime.now(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getManagedBooking(Long id, boolean admin) {
        if (admin) {
            requireAdmin();
        }
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking"));
        checkCourtManagement(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelManagedBooking(Long id, String reason) {
        requireReason(reason);
        Booking booking = findBookingForUpdate(id);
        checkCourtManagement(booking);
        requireLiveHold(booking);
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Chỉ có thể hủy booking đang hoạt động");
        }
        if (!LocalDateTime.of(booking.getBookingDate(), booking.getEndTime()).isAfter(BookingTime.now())) {
            throw new ConflictException("Không thể hủy sau khi booking đã kết thúc");
        }
        paymentRepository.findByBookingId(id).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                throw new ConflictException("Payment cần được đối soát trước khi hủy booking");
            }
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            paymentRepository.save(payment);
        });
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason.trim());
        booking.setCancelledBy(SecurityContextHolder.getContext().getAuthentication().getName());
        booking.setCancelledAt(BookingTime.now());
        bookingRepository.saveAndFlush(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse markNoShow(Long id, String reason) {
        requireReason(reason);
        Booking booking = findBookingForUpdate(id);
        checkCourtManagement(booking);
        if (booking.getStatus() != BookingStatus.CONFIRMED
                || LocalDateTime.of(booking.getBookingDate(), booking.getEndTime()).isAfter(BookingTime.now())
                || !paymentRepository.existsByBookingIdAndStatus(id, PaymentStatus.COMPLETED)) {
            throw new ConflictException("Chỉ ghi nhận vắng khi booking đã thanh toán, xác nhận và kết thúc");
        }
        booking.setStatus(BookingStatus.NO_SHOW);
        booking.setOutcomeReason(reason.trim());
        booking.setResolvedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        booking.setResolvedAt(BookingTime.now());
        bookingRepository.saveAndFlush(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse recordRefund(Long id, String reference) {
        requireAdmin();
        if (reference == null || reference.isBlank() || reference.length() > 200) {
            throw new IllegalArgumentException("Mã tham chiếu hoàn tiền bắt buộc, tối đa 200 ký tự");
        }
        Booking booking = findBookingForUpdate(id);
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new ConflictException("Chỉ booking đã hủy mới được ghi nhận hoàn tiền");
        }
        com.sporthub.entity.Payment payment = paymentRepository.findByBookingId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new ConflictException("Payment không ở trạng thái chờ hoàn tiền");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundReference(reference.trim());
        payment.setRefundedAt(BookingTime.now());
        payment.setRefundedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        paymentRepository.saveAndFlush(payment);
        return toResponse(booking);
    }

    private void requireAdmin() {
        boolean admin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin) {
            throw new AccessDeniedException("Chỉ Admin được thực hiện thao tác này");
        }
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new IllegalArgumentException("Lý do bắt buộc, tối đa 500 ký tự");
        }
    }

    private void validateOpeningHours(Court court, CreateBookingRequest request) {
        CourtSchedule schedule = courtScheduleRepository
                .findByCourt_IdAndDayOfWeek(court.getId(), request.getBookingDate().getDayOfWeek())
                .orElse(null);
        if (schedule != null && !Boolean.TRUE.equals(schedule.getAvailable())) {
            throw new ConflictException("Sân không nhận đặt vào ngày này");
        }
        LocalTime openTime = schedule == null ? court.getOpenTime() : schedule.getOpenTime();
        LocalTime closeTime = schedule == null ? court.getCloseTime() : schedule.getCloseTime();
        if (openTime == null || closeTime == null || !openTime.isBefore(closeTime)) {
            throw new ConflictException("Sân chưa cấu hình giờ hoạt động hợp lệ");
        }
        if (request.getStartTime().isBefore(openTime) || request.getEndTime().isAfter(closeTime)) {
            throw new ConflictException("Khung giờ đặt nằm ngoài giờ hoạt động của sân");
        }
    }

    private void requireFutureStart(LocalDateTime startAt) {
        if (!startAt.isAfter(BookingTime.now())) {
            throw new ConflictException("Thời gian bắt đầu phải ở tương lai");
        }
    }

    private Booking findBookingForUpdate(Long id) {
        return bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking có id = " + id));
    }

    private void checkOwnership(Booking booking) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!booking.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập booking này");
        }
    }

    private void checkCourtManagement(Booking booking) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            return;
        }
        Court court = courtRepository.findById(booking.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));
        if (!court.getVenue().getOwner().getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("Bạn không sở hữu sân của booking này");
        }
    }

    private void requireLiveHold(Booking booking) {
        if (BookingTime.expired(booking, BookingTime.now())) {
            throw new ConflictException("Booking hold has expired");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        BookingStatus visibleStatus = BookingTime.expired(booking, BookingTime.now())
                ? BookingStatus.EXPIRED : booking.getStatus();
        BookingResponse response = new BookingResponse(booking.getId(), booking.getCourtId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), booking.getTotalAmount(),
                visibleStatus, booking.getCreatedAt(), booking.getUpdatedAt());
        response.setExpiresAt(booking.getExpiresAt());
        response.setBookingCode(booking.getBookingCode());
        response.setUnitPrice(booking.getUnitPrice());
        response.setCancellationReason(booking.getCancellationReason());
        response.setCancelledAt(booking.getCancelledAt());
        response.setCancelledBy(booking.getCancelledBy());
        response.setOutcomeReason(booking.getOutcomeReason());
        response.setResolvedAt(booking.getResolvedAt());
        response.setResolvedBy(booking.getResolvedBy());
        response.setCustomerUsername(booking.getUser().getUsername());
        paymentRepository.findByBookingId(booking.getId()).ifPresent(payment -> {
            response.setPaymentStatus(payment.getStatus());
            response.setRefundReference(payment.getRefundReference());
        });
        Court court = courtRepository.findById(booking.getCourtId()).orElse(null);
        if (court != null) {
            response.setCourtName(court.getName());
            response.setVenueName(court.getVenue().getName());
        }
        return response;
    }
}
