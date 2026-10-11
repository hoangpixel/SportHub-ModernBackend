package com.sporthub.service;

import com.sporthub.dto.request.CourtRequest;
import com.sporthub.dto.response.CourtResponse;
import com.sporthub.entity.Court;
import com.sporthub.entity.Sport;
import com.sporthub.entity.User;
import com.sporthub.entity.Venue;
import com.sporthub.enums.CourtStatus;
import com.sporthub.enums.VenueStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.CourtRepository;
import com.sporthub.repository.SportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourtServiceTest {

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private SportRepository sportRepository;

    @Mock
    private VenueService venueService;

    @InjectMocks
    private CourtService courtService;

    private User owner;
    private Venue venue;
    private Sport sport;
    private Court court;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner01");

        venue = new Venue();
        venue.setId(10L);
        venue.setName("SportHub Arena");
        venue.setOwner(owner);

        sport = new Sport();
        sport.setId(2L);
        sport.setName("Football");

        court = new Court();
        court.setId(100L);
        court.setName("Sân A");
        court.setVenue(venue);
        court.setSport(sport);
        court.setPricePerHour(new BigDecimal("200000"));
        court.setStatus(CourtStatus.ACTIVE);
    }

    @Test
    void createCourt_Success() {
        CourtRequest request = new CourtRequest();
        request.setName("Sân B");
        request.setSportId(2L);
        request.setPricePerHour(new BigDecimal("200000"));
        request.setOpenTime(LocalTime.of(6, 0));
        request.setCloseTime(LocalTime.of(22, 0));

        when(venueService.findVenueById(10L)).thenReturn(venue);
        when(courtRepository.existsByNameIgnoreCaseAndVenue_Id("Sân B", 10L)).thenReturn(false);
        when(sportRepository.findById(2L)).thenReturn(Optional.of(sport));
        when(courtRepository.save(any(Court.class))).thenAnswer(invocation -> {
            Court c = invocation.getArgument(0);
            c.setId(101L);
            return c;
        });

        CourtResponse response = courtService.createCourt("owner01", 10L, request);

        assertNotNull(response);
        assertEquals("Sân B", response.getName());
        verify(courtRepository, times(1)).save(any(Court.class));
    }

    @Test
    void createCourt_DuplicateName_ThrowsConflictException() {
        CourtRequest request = new CourtRequest();
        request.setName("Sân A");

        when(venueService.findVenueById(10L)).thenReturn(venue);
        when(courtRepository.existsByNameIgnoreCaseAndVenue_Id("Sân A", 10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> courtService.createCourt("owner01", 10L, request));
    }

    @Test
    void createCourt_NotOwner_ThrowsAccessDeniedException() {
        CourtRequest request = new CourtRequest();
        request.setName("Sân B");

        when(venueService.findVenueById(10L)).thenReturn(venue);

        assertThrows(AccessDeniedException.class, () -> courtService.createCourt("other_user", 10L, request));
    }
}
