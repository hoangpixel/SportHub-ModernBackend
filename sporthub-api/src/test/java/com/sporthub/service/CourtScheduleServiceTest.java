package com.sporthub.service;

import com.sporthub.dto.request.CourtScheduleRequest;
import com.sporthub.dto.response.CourtScheduleResponse;
import com.sporthub.entity.Court;
import com.sporthub.entity.CourtSchedule;
import com.sporthub.entity.User;
import com.sporthub.entity.Venue;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.CourtScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourtScheduleServiceTest {

    @Mock
    private CourtScheduleRepository courtScheduleRepository;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private CourtScheduleService courtScheduleService;

    private User owner;
    private Venue venue;
    private Court court;
    private CourtSchedule schedule;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner01");

        venue = new Venue();
        venue.setId(10L);
        venue.setOwner(owner);

        court = new Court();
        court.setId(100L);
        court.setVenue(venue);

        schedule = new CourtSchedule();
        schedule.setId(1000L);
        schedule.setCourt(court);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setOpenTime(LocalTime.of(8, 0));
        schedule.setCloseTime(LocalTime.of(22, 0));
        schedule.setAvailable(true);
    }

    @Test
    void getSchedulesByCourt_Success() {
        when(courtService.findCourtById(100L)).thenReturn(court);
        when(courtScheduleRepository.findByCourt_IdOrderByDayOfWeek(100L))
                .thenReturn(List.of(schedule));

        List<CourtScheduleResponse> result = courtScheduleService.getSchedulesByCourt(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(DayOfWeek.MONDAY, result.get(0).getDayOfWeek());
    }

    @Test
    void createSchedule_Success() {
        CourtScheduleRequest request = new CourtScheduleRequest();
        request.setDayOfWeek(DayOfWeek.TUESDAY);
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(20, 0));
        request.setAvailable(true);

        when(courtService.findCourtById(100L)).thenReturn(court);
        when(courtScheduleRepository.existsByCourt_IdAndDayOfWeek(100L, DayOfWeek.TUESDAY)).thenReturn(false);

        CourtScheduleResponse response = courtScheduleService.createSchedule("owner01", 100L, request);

        assertNotNull(response);
        assertEquals(DayOfWeek.TUESDAY, response.getDayOfWeek());
        verify(courtScheduleRepository, times(1)).save(any(CourtSchedule.class));
    }

    @Test
    void createSchedule_NotOwner_ThrowsAccessDeniedException() {
        CourtScheduleRequest request = new CourtScheduleRequest();
        request.setDayOfWeek(DayOfWeek.TUESDAY);
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(20, 0));

        when(courtService.findCourtById(100L)).thenReturn(court);

        assertThrows(AccessDeniedException.class, () ->
                courtScheduleService.createSchedule("other_user", 100L, request));
    }

    @Test
    void createSchedule_InvalidTime_ThrowsIllegalArgumentException() {
        CourtScheduleRequest request = new CourtScheduleRequest();
        request.setDayOfWeek(DayOfWeek.TUESDAY);
        request.setOpenTime(LocalTime.of(22, 0));
        request.setCloseTime(LocalTime.of(8, 0));

        when(courtService.findCourtById(100L)).thenReturn(court);
        when(courtScheduleRepository.existsByCourt_IdAndDayOfWeek(100L, DayOfWeek.TUESDAY)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                courtScheduleService.createSchedule("owner01", 100L, request));
    }

    @Test
    void createSchedule_DuplicateDay_ThrowsConflictException() {
        CourtScheduleRequest request = new CourtScheduleRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(22, 0));

        when(courtService.findCourtById(100L)).thenReturn(court);
        when(courtScheduleRepository.existsByCourt_IdAndDayOfWeek(100L, DayOfWeek.MONDAY)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                courtScheduleService.createSchedule("owner01", 100L, request));
    }

    @Test
    void deleteSchedule_Success() {
        when(courtScheduleRepository.findById(1000L)).thenReturn(Optional.of(schedule));

        courtScheduleService.deleteSchedule("owner01", 1000L);

        verify(courtScheduleRepository, times(1)).delete(schedule);
    }
}
