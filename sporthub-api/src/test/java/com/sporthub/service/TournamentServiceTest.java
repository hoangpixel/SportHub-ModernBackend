package com.sporthub.service;

import com.sporthub.dto.request.CreateTournamentRequest;
import com.sporthub.dto.request.UpdateTournamentRequest;
import com.sporthub.dto.response.TournamentResponse;
import com.sporthub.entity.Tournament;
import com.sporthub.entity.User;
import com.sporthub.enums.TournamentStatus;
import com.sporthub.enums.UserStatus;
import com.sporthub.repository.SportRepository;
import com.sporthub.repository.TournamentRepository;
import com.sporthub.security.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    TournamentRepository tournamentRepository;

    @Mock
    SportRepository sportRepository;

    @Mock
    CurrentUserService currentUserService;

    @InjectMocks
    TournamentService tournamentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test",
                        null,
                        Arrays.stream(authorities)
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                )
        );
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@gmail.com");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private CreateTournamentRequest createRequest() {
        CreateTournamentRequest request =
                mock(CreateTournamentRequest.class);

        when(request.getSportId()).thenReturn(1L);
        when(request.getVenueId()).thenReturn(1L);
        when(request.getName()).thenReturn("Football Cup");
        when(request.getDescription()).thenReturn("Tournament");

        when(request.getStartDate())
                .thenReturn(LocalDate.of(2026, 12, 20));

        when(request.getEndDate())
                .thenReturn(LocalDate.of(2026, 12, 21));

        when(request.getRegistrationStart())
                .thenReturn(LocalDate.of(2026, 12, 1));

        when(request.getRegistrationEnd())
                .thenReturn(LocalDate.of(2026, 12, 15));

        when(request.getMaxTeams()).thenReturn(8);

        return request;
    }

    @Test
    void createTournament_shouldSuccessForAdmin() {
        setAuthentication("ROLE_ADMIN");

        User admin = createUser(1L, "admin");

        when(currentUserService.getCurrentUser())
                .thenReturn(admin);

        when(sportRepository.existsById(1L))
                .thenReturn(true);

        Tournament tournament = Tournament.builder()
                .id(10L)
                .createdBy(1L)
                .sportId(1L)
                .name("Football Cup")
                .status(TournamentStatus.UPCOMING)
                .build();

        when(tournamentRepository.save(any(Tournament.class)))
                .thenReturn(tournament);

        TournamentResponse response =
                tournamentService.createTournament(
                        createRequest());

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Football Cup", response.getName());

        verify(tournamentRepository)
                .save(any(Tournament.class));
    }

    @Test
    void createTournament_shouldRejectNonAdmin() {
        setAuthentication("ROLE_USER");

        CreateTournamentRequest request =
                mock(CreateTournamentRequest.class);

        assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.createTournament(request)
        );

        verify(tournamentRepository, never())
                .save(any(Tournament.class));
    }

    @Test
    void updateTournament_shouldSuccessForOwner() {
        setAuthentication("MANAGE_TOURNAMENTS");

        User owner = createUser(1L, "owner");

        Tournament tournament = Tournament.builder()
                .id(10L)
                .createdBy(1L)
                .sportId(1L)
                .name("Old Name")
                .description("Old")
                .startDate(LocalDate.of(2026, 12, 20))
                .endDate(LocalDate.of(2026, 12, 21))
                .registrationStart(LocalDate.of(2026, 12, 1))
                .registrationEnd(LocalDate.of(2026, 12, 15))
                .maxTeams(8)
                .status(TournamentStatus.UPCOMING)
                .build();

        UpdateTournamentRequest request =
                mock(UpdateTournamentRequest.class);

        when(request.getName()).thenReturn("New Name");
        when(request.getDescription()).thenReturn("New");

        when(request.getStartDate())
                .thenReturn(LocalDate.of(2026, 12, 20));

        when(request.getEndDate())
                .thenReturn(LocalDate.of(2026, 12, 22));

        when(request.getRegistrationStart())
                .thenReturn(LocalDate.of(2026, 12, 1));

        when(request.getRegistrationEnd())
                .thenReturn(LocalDate.of(2026, 12, 15));

        when(request.getMaxTeams()).thenReturn(16);

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(tournament));

        when(currentUserService.getCurrentUser())
                .thenReturn(owner);

        when(tournamentRepository.save(any(Tournament.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        TournamentResponse response =
                tournamentService.updateTournament(
                        10L,
                        request);

        assertEquals("New Name", response.getName());
        assertEquals(16, response.getMaxTeams());

        verify(tournamentRepository)
                .save(tournament);
    }

    @Test
    void updateTournament_shouldRejectUnauthorizedUser() {
        setAuthentication("MANAGE_TOURNAMENTS");

        User otherUser = createUser(2L, "user2");

        Tournament tournament = Tournament.builder()
                .id(10L)
                .createdBy(1L)
                .build();

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(tournament));

        when(currentUserService.getCurrentUser())
                .thenReturn(otherUser);

        UpdateTournamentRequest request =
                mock(UpdateTournamentRequest.class);

        assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.updateTournament(
                        10L,
                        request)
        );

        verify(tournamentRepository, never())
                .save(any(Tournament.class));
    }
}
