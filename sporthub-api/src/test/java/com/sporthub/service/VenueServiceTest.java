package com.sporthub.service;

import com.sporthub.dto.request.VenueRequest;
import com.sporthub.dto.response.VenueResponse;
import com.sporthub.entity.User;
import com.sporthub.entity.Venue;
import com.sporthub.enums.VenueStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.UserRepository;
import com.sporthub.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VenueService venueService;

    private User owner;
    private Venue venue;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner01");

        venue = new Venue();
        venue.setId(10L);
        venue.setName("SportHub Arena");
        venue.setAddress("123 Street");
        venue.setCity("HCMC");
        venue.setStatus(VenueStatus.ACTIVE);
        venue.setOwner(owner);
    }

    @Test
    void createVenue_Success() {
        VenueRequest request = new VenueRequest();
        request.setName("New Arena");
        request.setAddress("123 Street");

        when(userRepository.findByUsername("owner01")).thenReturn(Optional.of(owner));
        when(venueRepository.existsByNameIgnoreCaseAndOwner_Id("New Arena", 1L)).thenReturn(false);
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> {
            Venue v = invocation.getArgument(0);
            v.setId(11L);
            return v;
        });

        VenueResponse response = venueService.createVenue("owner01", request);

        assertNotNull(response);
        assertEquals("New Arena", response.getName());
        verify(venueRepository, times(1)).save(any(Venue.class));
    }

    @Test
    void createVenue_DuplicateName_ThrowsConflictException() {
        VenueRequest request = new VenueRequest();
        request.setName("SportHub Arena");

        when(userRepository.findByUsername("owner01")).thenReturn(Optional.of(owner));
        when(venueRepository.existsByNameIgnoreCaseAndOwner_Id("SportHub Arena", 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> venueService.createVenue("owner01", request));
    }

    @Test
    void getVenueById_NotFound_ThrowsResourceNotFoundException() {
        when(venueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> venueService.getVenueById(99L));
    }

    @Test
    void deleteVenue_NotOwner_ThrowsAccessDeniedException() {
        when(venueRepository.findById(10L)).thenReturn(Optional.of(venue));

        assertThrows(AccessDeniedException.class, () -> venueService.deleteVenue("other_user", 10L));
    }

    @Test
    void deleteVenue_Success() {
        when(venueRepository.findById(10L)).thenReturn(Optional.of(venue));

        venueService.deleteVenue("owner01", 10L);

        verify(venueRepository, times(1)).delete(venue);
    }
}
