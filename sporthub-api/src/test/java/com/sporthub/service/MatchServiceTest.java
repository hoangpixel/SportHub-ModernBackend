package com.sporthub.service;

import com.sporthub.dto.request.CreateMatchRequest;
import com.sporthub.dto.request.UpdateMatchResultRequest;
import com.sporthub.dto.response.MatchResponse;
import com.sporthub.entity.Match;
import com.sporthub.entity.Tournament;
import com.sporthub.entity.User;
import com.sporthub.enums.MatchStatus;
import com.sporthub.enums.UserStatus;
import com.sporthub.repository.MatchRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    MatchRepository matchRepository;

    @Mock
    TournamentRepository tournamentRepository;

    @Mock
    TournamentTeamRepository tournamentTeamRepository;

    @Mock
    TeamRepository teamRepository;

    @Mock
    CurrentUserService currentUserService;

    @InjectMocks
    MatchService matchService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "MANAGE_TOURNAMENTS")
                        )
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
                .build();
    }

    private CreateMatchRequest createRequest() {
        CreateMatchRequest request =
                mock(CreateMatchRequest.class);

        when(request.getTournamentId())
                .thenReturn(10L);

        when(request.getRoundNumber())
                .thenReturn(1);

        when(request.getCourtId())
                .thenReturn(5L);

        when(request.getTeam1Id())
                .thenReturn(20L);

        when(request.getTeam2Id())
                .thenReturn(30L);

        when(request.getScheduledDate())
                .thenReturn(LocalDate.of(2026, 12, 20));

        when(request.getStartTime())
                .thenReturn(LocalTime.of(9, 0));

        when(request.getEndTime())
                .thenReturn(LocalTime.of(10, 0));

        return request;
    }

    private void mockValidCreate() {
        setAuthentication();

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(createTournament()));

        when(currentUserService.getCurrentUser())
                .thenReturn(createUser(1L, "owner"));

        when(teamRepository.existsById(20L))
                .thenReturn(true);

        when(teamRepository.existsById(30L))
                .thenReturn(true);

        when(tournamentTeamRepository
                .existsByTournamentIdAndTeamId(10L, 20L))
                .thenReturn(true);

        when(tournamentTeamRepository
                .existsByTournamentIdAndTeamId(10L, 30L))
                .thenReturn(true);
    }

    @Test
    void createMatch_shouldSuccess() {
        mockValidCreate();

        when(matchRepository.existsConflict(
                5L,
                LocalDate.of(2026, 12, 20),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)))
                .thenReturn(false);

        Match saved = Match.builder()
                .id(100L)
                .tournamentId(10L)
                .roundNumber(1)
                .courtId(5L)
                .team1Id(20L)
                .team2Id(30L)
                .scheduledDate(
                        LocalDate.of(2026, 12, 20))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .team1Score(0)
                .team2Score(0)
                .status(MatchStatus.SCHEDULED)
                .build();

        when(matchRepository.save(any(Match.class)))
                .thenReturn(saved);

        MatchResponse response =
                matchService.createMatch(createRequest());

        assertNotNull(response);

        assertEquals(
                100L,
                response.getId());

        assertEquals(
                MatchStatus.SCHEDULED,
                response.getStatus());

        verify(matchRepository)
                .save(any(Match.class));
    }

    @Test
    void createMatch_shouldRejectSameTeams() {
        setAuthentication();

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(createTournament()));

        when(currentUserService.getCurrentUser())
                .thenReturn(createUser(1L, "owner"));

        CreateMatchRequest request =
                mock(CreateMatchRequest.class);

        when(request.getTournamentId())
                .thenReturn(10L);

        when(request.getTeam1Id())
                .thenReturn(20L);

        when(request.getTeam2Id())
                .thenReturn(20L);

        assertThrows(
                IllegalArgumentException.class,
                () -> matchService.createMatch(request)
        );

        verify(matchRepository, never())
                .save(any(Match.class));
    }

    @Test
    void createMatch_shouldRejectUnregisteredTeam() {
        setAuthentication();

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(createTournament()));

        when(currentUserService.getCurrentUser())
                .thenReturn(createUser(1L, "owner"));

        when(teamRepository.existsById(20L))
                .thenReturn(true);

        when(teamRepository.existsById(30L))
                .thenReturn(true);

        when(tournamentTeamRepository
                .existsByTournamentIdAndTeamId(10L, 20L))
                .thenReturn(true);

        when(tournamentTeamRepository
                .existsByTournamentIdAndTeamId(10L, 30L))
                .thenReturn(false);

        CreateMatchRequest request =
                mock(CreateMatchRequest.class);

        when(request.getTournamentId())
                .thenReturn(10L);

        when(request.getTeam1Id())
                .thenReturn(20L);

        when(request.getTeam2Id())
                .thenReturn(30L);

        assertThrows(
                IllegalStateException.class,
                () -> matchService.createMatch(request)
        );

        verify(matchRepository, never())
                .save(any(Match.class));
    }

    @Test
    void createMatch_shouldRejectCourtTimeConflict() {
        setAuthentication();

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(createTournament()));

        when(currentUserService.getCurrentUser())
                .thenReturn(createUser(1L, "owner"));

        when(teamRepository.existsById(20L))
                .thenReturn(true);

        when(teamRepository.existsById(30L))
                .thenReturn(true);

        when(tournamentTeamRepository
                .existsByTournamentIdAndTeamId(10L, 20L))
                .thenReturn(true);

        when(tournamentTeamRepository
                .existsByTournamentIdAndTeamId(10L, 30L))
                .thenReturn(true);

        CreateMatchRequest request =
                mock(CreateMatchRequest.class);

        when(request.getTournamentId())
                .thenReturn(10L);

        when(request.getTeam1Id())
                .thenReturn(20L);

        when(request.getTeam2Id())
                .thenReturn(30L);

        when(request.getCourtId())
                .thenReturn(5L);

        when(request.getScheduledDate())
                .thenReturn(LocalDate.of(2026, 12, 20));

        when(request.getStartTime())
                .thenReturn(LocalTime.of(9, 0));

        when(request.getEndTime())
                .thenReturn(LocalTime.of(10, 0));

        when(matchRepository.existsConflict(
                5L,
                LocalDate.of(2026, 12, 20),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> matchService.createMatch(request)
        );

        verify(matchRepository, never())
                .save(any(Match.class));
    }

    @Test
    void updateResult_shouldSetWinnerAndCompleted() {
        setAuthentication();

        Match match = Match.builder()
                .id(100L)
                .tournamentId(10L)
                .team1Id(20L)
                .team2Id(30L)
                .status(MatchStatus.SCHEDULED)
                .build();

        when(matchRepository.findById(100L))
                .thenReturn(Optional.of(match));

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(createTournament()));

        when(currentUserService.getCurrentUser())
                .thenReturn(createUser(1L, "owner"));

        UpdateMatchResultRequest request =
                mock(UpdateMatchResultRequest.class);

        when(request.getTeam1Score())
                .thenReturn(3);

        when(request.getTeam2Score())
                .thenReturn(1);

        when(matchRepository.save(any(Match.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        MatchResponse response =
                matchService.updateResult(
                        100L,
                        request);

        assertEquals(
                3,
                response.getTeam1Score());

        assertEquals(
                1,
                response.getTeam2Score());

        assertEquals(
                20L,
                response.getWinnerTeamId());

        assertEquals(
                MatchStatus.COMPLETED,
                response.getStatus());
    }

    @Test
    void updateResult_shouldSetWinnerNullWhenDraw() {
        setAuthentication();

        Match match = Match.builder()
                .id(100L)
                .tournamentId(10L)
                .team1Id(20L)
                .team2Id(30L)
                .status(MatchStatus.SCHEDULED)
                .build();

        when(matchRepository.findById(100L))
                .thenReturn(Optional.of(match));

        when(tournamentRepository.findById(10L))
                .thenReturn(Optional.of(createTournament()));

        when(currentUserService.getCurrentUser())
                .thenReturn(createUser(1L, "owner"));

        UpdateMatchResultRequest request =
                mock(UpdateMatchResultRequest.class);

        when(request.getTeam1Score())
                .thenReturn(2);

        when(request.getTeam2Score())
                .thenReturn(2);

        when(matchRepository.save(any(Match.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        MatchResponse response =
                matchService.updateResult(
                        100L,
                        request);

        assertNull(
                response.getWinnerTeamId());

        assertEquals(
                MatchStatus.COMPLETED,
                response.getStatus());

        assertEquals(
                2,
                response.getTeam1Score());

        assertEquals(
                2,
                response.getTeam2Score());
    }
}
