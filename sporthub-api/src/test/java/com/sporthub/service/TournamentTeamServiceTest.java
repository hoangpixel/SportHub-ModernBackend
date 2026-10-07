package com.sporthub.service;

import com.sporthub.dto.request.RegisterTournamentTeamRequest;
import com.sporthub.dto.response.TournamentTeamResponse;
import com.sporthub.entity.Team;
import com.sporthub.entity.Tournament;
import com.sporthub.entity.TournamentTeam;
import com.sporthub.entity.User;
import com.sporthub.enums.RegistrationStatus;
import com.sporthub.enums.UserStatus;
import com.sporthub.repository.TeamRepository;
import com.sporthub.repository.TournamentRepository;
import com.sporthub.repository.TournamentTeamRepository;
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

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentTeamServiceTest {

    @Mock
    TournamentRepository tournamentRepository;

    @Mock
    TournamentTeamRepository tournamentTeamRepository;

    @Mock
    TeamRepository teamRepository;

    @Mock
    CurrentUserService currentUserService;

    @InjectMocks
    TournamentTeamService tournamentTeamService;

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

    private Tournament createTournament() {
        return Tournament.builder()
                .id(10L)
                .createdBy(1L)
                .maxTeams(8)
                .build();
    }

    private Team createTeam() {
        return Team.builder()
                .id(20L)
                .ownerUserId(1L)
                .build();
    }

    private RegisterTournamentTeamRequest createRequest() {
        RegisterTournamentTeamRequest request =
                mock(RegisterTournamentTeamRequest.class);

        when(request.getTeamId()).thenReturn(20L);

        return request;
    }

    @Test
    void registerTeam_shouldSuccess() {
        setAuthentication("REGISTER_TOURNAMENT");

        User owner = createUser(1L, "owner");

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(createTournament()));

        when(teamRepository.findById(20L))
                .thenReturn(Optional.of(createTeam()));

        when(currentUserService.getCurrentUser())
                .thenReturn(owner);

        when(tournamentTeamRepository
                .existsByTournamentIdAndTeamId(10L, 20L))
                .thenReturn(false);

        when(tournamentTeamRepository
                .countByTournamentId(10L))
                .thenReturn(2L);

        TournamentTeam saved =
                TournamentTeam.builder()
                        .id(100L)
                        .tournamentId(10L)
                        .teamId(20L)
                        .registrationStatus(
                                RegistrationStatus.PENDING)
                        .build();

        when(tournamentTeamRepository.save(
                any(TournamentTeam.class)))
                .thenReturn(saved);

        TournamentTeamResponse response =
                tournamentTeamService.registerTeam(
                        10L,
                        createRequest());

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getTournamentId());
        assertEquals(20L, response.getTeamId());

        verify(tournamentTeamRepository)
                .save(any(TournamentTeam.class));
    }

    @Test
    void registerTeam_shouldRejectDuplicateRegistration() {
        setAuthentication("REGISTER_TOURNAMENT");

        User owner = createUser(1L, "owner");

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(createTournament()));

        when(teamRepository.findById(20L))
                .thenReturn(Optional.of(createTeam()));

        when(currentUserService.getCurrentUser())
                .thenReturn(owner);

        when(tournamentTeamRepository
                .existsByTournamentIdAndTeamId(10L, 20L))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> tournamentTeamService.registerTeam(
                        10L,
                        createRequest())
        );

        verify(tournamentTeamRepository, never())
                .save(any(TournamentTeam.class));
    }

    @Test
    void registerTeam_shouldRejectWhenTournamentIsFull() {
        setAuthentication("REGISTER_TOURNAMENT");

        User owner = createUser(1L, "owner");

        Tournament tournament = createTournament();
        tournament.setMaxTeams(2);

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(tournament));

        when(teamRepository.findById(20L))
                .thenReturn(Optional.of(createTeam()));

        when(currentUserService.getCurrentUser())
                .thenReturn(owner);

        when(tournamentTeamRepository
                .existsByTournamentIdAndTeamId(10L, 20L))
                .thenReturn(false);

        when(tournamentTeamRepository
                .countByTournamentId(10L))
                .thenReturn(2L);

        assertThrows(
                IllegalStateException.class,
                () -> tournamentTeamService.registerTeam(
                        10L,
                        createRequest())
        );

        verify(tournamentTeamRepository, never())
                .save(any(TournamentTeam.class));
    }

    @Test
    void unregisterTeam_shouldRejectUnauthorizedUser() {
        setAuthentication("REGISTER_TOURNAMENT");

        User otherUser = createUser(99L, "other");

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(createTournament()));

        when(teamRepository.findById(20L))
                .thenReturn(Optional.of(createTeam()));

        when(currentUserService.getCurrentUser())
                .thenReturn(otherUser);

        assertThrows(
                AccessDeniedException.class,
                () -> tournamentTeamService.unregisterTeam(
                        10L,
                        20L)
        );

        verify(tournamentTeamRepository, never())
                .delete(any(TournamentTeam.class));
    }
}
