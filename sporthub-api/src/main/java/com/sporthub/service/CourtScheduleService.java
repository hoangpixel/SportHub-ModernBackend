package com.sporthub.service;

import com.sporthub.dto.request.CourtScheduleRequest;
import com.sporthub.dto.response.CourtScheduleResponse;
import com.sporthub.entity.Court;
import com.sporthub.entity.CourtSchedule;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.CourtScheduleRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourtScheduleService {

    private final CourtScheduleRepository courtScheduleRepository;
    private final CourtService courtService;

    @Transactional(readOnly = true)
    public void requireScheduleInCourt(Long scheduleId, Long courtId) {
        if (!findScheduleById(scheduleId).getCourt().getId().equals(courtId)) {
            throw new ResourceNotFoundException("Lịch không thuộc sân trong đường dẫn");
        }
    }

    @Transactional(readOnly = true)
    public List<CourtScheduleResponse> getSchedulesByCourt(Long courtId) {
        courtService.findCourtById(courtId);
        return courtScheduleRepository
                .findByCourt_IdOrderByDayOfWeek(courtId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CourtScheduleResponse createSchedule(
            String username,
            Long courtId,
            CourtScheduleRequest request) {

        Court court = courtService.findCourtById(courtId);
        checkOwnership(court, username);

        if (courtScheduleRepository.existsByCourt_IdAndDayOfWeek(
                courtId, request.getDayOfWeek())) {
            throw new ConflictException(
                    "Lịch cho ngày " + request.getDayOfWeek()
                    + " đã tồn tại");
        }

        if (!request.getOpenTime().isBefore(request.getCloseTime())) {
            throw new IllegalArgumentException(
                    "Giờ mở cửa phải trước giờ đóng cửa");
        }

        CourtSchedule schedule = new CourtSchedule();
        schedule.setCourt(court);
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setOpenTime(request.getOpenTime());
        schedule.setCloseTime(request.getCloseTime());
        schedule.setAvailable(
                request.getAvailable() != null
                        ? request.getAvailable()
                        : true);

        courtScheduleRepository.save(schedule);

        return toResponse(schedule);
    }

    @Transactional
    public CourtScheduleResponse updateSchedule(
            String username,
            Long scheduleId,
            CourtScheduleRequest request) {

        CourtSchedule schedule = findScheduleById(scheduleId);
        checkOwnership(schedule.getCourt(), username);

        if (!request.getOpenTime().isBefore(request.getCloseTime())) {
            throw new IllegalArgumentException(
                    "Giờ mở cửa phải trước giờ đóng cửa");
        }

        if (request.getDayOfWeek() != schedule.getDayOfWeek()) {
            boolean exists = courtScheduleRepository
                    .existsByCourt_IdAndDayOfWeek(
                            schedule.getCourt().getId(),
                            request.getDayOfWeek());
            if (exists) {
                throw new ConflictException(
                        "Lịch cho ngày " + request.getDayOfWeek()
                        + " đã tồn tại");
            }
        }

        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setOpenTime(request.getOpenTime());
        schedule.setCloseTime(request.getCloseTime());
        schedule.setAvailable(
                request.getAvailable() != null
                        ? request.getAvailable()
                        : true);

        courtScheduleRepository.saveAndFlush(schedule);

        return toResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(String username, Long scheduleId) {
        CourtSchedule schedule = findScheduleById(scheduleId);
        checkOwnership(schedule.getCourt(), username);
        courtScheduleRepository.delete(schedule);
    }

    private CourtSchedule findScheduleById(Long id) {
        return courtScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lịch sân có id = " + id));
    }

    private void checkOwnership(Court court, String username) {
        if (!court.getVenue().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "Bạn không có quyền thao tác trên sân này");
        }
    }

    private CourtScheduleResponse toResponse(CourtSchedule schedule) {
        return new CourtScheduleResponse(
                schedule.getId(),
                schedule.getCourt().getId(),
                schedule.getDayOfWeek(),
                schedule.getOpenTime(),
                schedule.getCloseTime(),
                schedule.getAvailable());
    }
}
