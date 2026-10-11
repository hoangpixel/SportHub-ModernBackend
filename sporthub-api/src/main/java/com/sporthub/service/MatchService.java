package com.sporthub.service;

import com.sporthub.constant.Permissions;
import com.sporthub.constant.Roles;
import com.sporthub.dto.request.CreateMatchRequest;
import com.sporthub.dto.request.UpdateMatchResultRequest;
import com.sporthub.dto.response.MatchResponse;
import com.sporthub.entity.Match;
import com.sporthub.entity.Tournament;
import com.sporthub.entity.User;
import com.sporthub.enums.MatchStatus;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.MatchRepository;
import com.sporthub.repository.TeamRepository;
import com.sporthub.repository.TournamentRepository;
import com.sporthub.repository.TournamentTeamRepository;
import com.sporthub.security.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MatchService {

    MatchRepository matchRepository;
    TournamentRepository tournamentRepository;
    TournamentTeamRepository tournamentTeamRepository;
    TeamRepository teamRepository;
    CurrentUserService currentUserService;

    @Transactional
    public MatchResponse createMatch(
            CreateMatchRequest request) {

        Tournament tournament =
                tournamentRepository.findById(
                                request.getTournamentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy giải đấu"));

        checkTournamentManager(tournament);

        if (request.getTeam1Id()
                .equals(request.getTeam2Id())) {

            throw new IllegalArgumentException(
                    "Hai đội không được giống nhau");
        }

        if (!teamRepository.existsById(request.getTeam1Id())
                || !teamRepository.existsById(request.getTeam2Id())) {

            throw new ResourceNotFoundException(
                    "Không tìm thấy đội");
        }

        checkTeamRegistered(
                request.getTournamentId(),
                request.getTeam1Id());

        checkTeamRegistered(
                request.getTournamentId(),
                request.getTeam2Id());

        if (!request.getStartTime()
                .isBefore(request.getEndTime())) {

            throw new IllegalArgumentException(
                    "Thời gian trận đấu không hợp lệ");
        }

        if (matchRepository.existsConflict(
                request.getCourtId(),
                request.getScheduledDate(),
                request.getStartTime(),
                request.getEndTime())) {

            throw new IllegalStateException(
                    "Court đã có trận đấu trong khoảng thời gian này");
        }

        LocalDateTime now = LocalDateTime.now();

        Match match = Match.builder()
                .tournamentId(request.getTournamentId())
                .roundNumber(request.getRoundNumber())
                .courtId(request.getCourtId())
                .team1Id(request.getTeam1Id())
                .team2Id(request.getTeam2Id())
                .scheduledDate(request.getScheduledDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .team1Score(0)
                .team2Score(0)
                .status(MatchStatus.SCHEDULED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(
                matchRepository.save(match));
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatch(Long matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy trận đấu"));

        return toResponse(match);
    }

    @Transactional
    public MatchResponse updateResult(
            Long matchId,
            UpdateMatchResultRequest request) {

        Match match =
                matchRepository.findById(matchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy trận đấu"));

        Tournament tournament =
                tournamentRepository.findById(
                                match.getTournamentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy giải đấu"));

        checkTournamentManager(tournament);

        match.setTeam1Score(request.getTeam1Score());
        match.setTeam2Score(request.getTeam2Score());

        if (request.getTeam1Score()
                > request.getTeam2Score()) {

            match.setWinnerTeamId(match.getTeam1Id());

        } else if (request.getTeam2Score()
                > request.getTeam1Score()) {

            match.setWinnerTeamId(match.getTeam2Id());

        } else {

            match.setWinnerTeamId(null);
        }

        match.setStatus(MatchStatus.COMPLETED);
        match.setUpdatedAt(LocalDateTime.now());

        return toResponse(
                matchRepository.save(match));
    }

    private void checkTeamRegistered(
            Long tournamentId,
            Long teamId) {

        if (!tournamentTeamRepository
                .existsByTournamentIdAndTeamId(
                        tournamentId,
                        teamId)) {

            throw new IllegalStateException(
                    "Đội chưa đăng ký giải đấu");
        }
    }

    private void checkTournamentManager(
            Tournament tournament) {

        if (isAdmin()) {
            return;
        }

        checkPermission(
                Permissions.MANAGE_TOURNAMENTS);

        User currentUser =
                currentUserService.getCurrentUser();

        if (!tournament.getCreatedBy()
                .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "Bạn không có quyền quản lý giải đấu này");
        }
    }

    private void checkPermission(String permission) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        boolean hasPermission =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals(permission));

        if (!hasPermission && !isAdmin()) {
            throw new AccessDeniedException(
                    "Bạn không có quyền thực hiện thao tác này");
        }
    }

    private boolean isAdmin() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_" + Roles.ADMIN));
    }

    private MatchResponse toResponse(Match match) {

        return MatchResponse.builder()
                .id(match.getId())
                .tournamentId(match.getTournamentId())
                .roundNumber(match.getRoundNumber())
                .courtId(match.getCourtId())
                .team1Id(match.getTeam1Id())
                .team2Id(match.getTeam2Id())
                .scheduledDate(match.getScheduledDate())
                .startTime(match.getStartTime())
                .endTime(match.getEndTime())
                .team1Score(match.getTeam1Score())
                .team2Score(match.getTeam2Score())
                .winnerTeamId(match.getWinnerTeamId())
                .status(match.getStatus())
                .createdAt(match.getCreatedAt())
                .updatedAt(match.getUpdatedAt())
                .build();
    }

    @Transactional
    public MatchResponse cancelMatch(Long matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy trận đấu"));

        Tournament tournament =
                tournamentRepository.findById(match.getTournamentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy giải đấu"));

        checkTournamentManager(tournament);

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Không thể hủy trận đấu đã hoàn thành");
        }

        match.setStatus(MatchStatus.CANCELLED);
        match.setUpdatedAt(LocalDateTime.now());

        return toResponse(matchRepository.save(match));
    }
}
