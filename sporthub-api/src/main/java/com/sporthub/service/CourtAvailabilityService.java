package com.sporthub.service;

import com.sporthub.dto.response.CourtAvailabilityResponse;
import com.sporthub.dto.response.CourtAvailabilityResponse.TimeRange;
import com.sporthub.entity.Booking;
import com.sporthub.entity.Court;
import com.sporthub.entity.CourtSchedule;
import com.sporthub.enums.CourtStatus;
import com.sporthub.enums.VenueStatus;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.CourtRepository;
import com.sporthub.repository.CourtScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourtAvailabilityService {
    private final CourtRepository courts;
    private final CourtScheduleRepository schedules;
    private final BookingRepository bookings;

    @Transactional(readOnly = true)
    public CourtAvailabilityResponse get(Long courtId, LocalDate date) {
        LocalDateTime now = BookingTime.now();
        Court court = courts.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));
        if (date.isBefore(now.toLocalDate())) {
            return closed(courtId, date, now, "Ngày yêu cầu đã ở trong quá khứ");
        }
        if (court.getStatus() != CourtStatus.ACTIVE || court.getVenue().getStatus() != VenueStatus.ACTIVE) {
            return closed(courtId, date, now, "Sân hoặc cơ sở hiện không hoạt động");
        }
        CourtSchedule schedule = schedules.findByCourt_IdAndDayOfWeek(courtId, date.getDayOfWeek()).orElse(null);
        if (schedule != null && !Boolean.TRUE.equals(schedule.getAvailable())) {
            return closed(courtId, date, now, "Sân không hoạt động trong ngày này");
        }
        LocalTime opening = schedule == null ? court.getOpenTime() : schedule.getOpenTime();
        LocalTime closing = schedule == null ? court.getCloseTime() : schedule.getCloseTime();
        if (opening == null || closing == null || !opening.isBefore(closing)) {
            return closed(courtId, date, now, "Sân chưa được cấu hình giờ hoạt động hợp lệ");
        }
        if (court.getPricePerHour() == null || court.getPricePerHour().signum() <= 0) {
            return closed(courtId, date, now, "Sân chưa được cấu hình đơn giá hợp lệ");
        }
        if (date.equals(now.toLocalDate())) {
            LocalDateTime nextSecond = now.withNano(0).plusSeconds(1);
            if (!nextSecond.toLocalDate().equals(date) || !nextSecond.toLocalTime().isBefore(closing)) {
                return closed(courtId, date, now, "Không còn giờ hoạt động nào trong tương lai");
            }
            if (nextSecond.toLocalTime().isAfter(opening)) {
                opening = nextSecond.toLocalTime();
            }
        }
        List<TimeRange> occupied = new ArrayList<>();
        List<TimeRange> available = new ArrayList<>();
        LocalTime cursor = opening;
        for (Booking booking : bookings.findOccupying(courtId, date, now)) {
            LocalTime start = booking.getStartTime().isBefore(opening) ? opening : booking.getStartTime();
            LocalTime end = booking.getEndTime().isAfter(closing) ? closing : booking.getEndTime();
            if (!start.isBefore(end)) {
                continue;
            }
            occupied.add(new TimeRange(start, end));
            if (cursor.isBefore(start)) {
                available.add(new TimeRange(cursor, start));
            }
            if (end.isAfter(cursor)) {
                cursor = end;
            }
        }
        if (cursor.isBefore(closing)) {
            available.add(new TimeRange(cursor, closing));
        }
        return new CourtAvailabilityResponse(courtId, date, now, !available.isEmpty(),
                available.isEmpty() ? "Không còn khung giờ trống" : null, occupied, available);
    }

    private CourtAvailabilityResponse closed(Long courtId, LocalDate date, LocalDateTime now, String reason) {
        return new CourtAvailabilityResponse(courtId, date, now, false, reason, List.of(), List.of());
    }
}
