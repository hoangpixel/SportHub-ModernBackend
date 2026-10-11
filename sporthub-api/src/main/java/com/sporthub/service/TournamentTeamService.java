package com.sporthub.service;

import com.sporthub.constant.Permissions;
import com.sporthub.constant.Roles;
import com.sporthub.dto.request.RegisterTournamentTeamRequest;
import com.sporthub.dto.response.TournamentTeamResponse;
import com.sporthub.entity.Team;
import com.sporthub.entity.Tournament;
import com.sporthub.entity.TournamentTeam;
import com.sporthub.entity.User;
import com.sporthub.enums.RegistrationStatus;
import com.sporthub.exception.ResourceNotFoundException;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class TournamentTeamService {

    TournamentRepository tournamentRepository;
    TournamentTeamRepository tournamentTeamRepository;
    TeamRepository teamRepository;
    CurrentUserService currentUserService;

    @Transactional
    public TournamentTeamResponse registerTeam(
            Long tournamentId,
            RegisterTournamentTeamRequest request) {

        Tournament tournament =
                tournamentRepository.findById(tournamentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy giải đấu"));

        Team team =
                teamRepository.findById(request.getTeamId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy đội"));

        checkRegisterPermission(team);

        if (tournamentTeamRepository
                .existsByTournamentIdAndTeamId(
                        tournamentId,
                        team.getId())) {

            throw new IllegalStateException(
                    "Đội đã đăng ký giải đấu");
        }

        long registeredTeams =
                tournamentTeamRepository
                        .countByTournamentId(tournamentId);

        if (registeredTeams >= tournament.getMaxTeams()) {
            throw new IllegalStateException(
                    "Giải đấu đã đủ số đội");
        }

        TournamentTeam tournamentTeam =
                TournamentTeam.builder()
                        .tournamentId(tournamentId)
                        .teamId(team.getId())
                        .registrationStatus(
                                RegistrationStatus.PENDING)
                        .registeredAt(LocalDateTime.now())
                        .build();

        return toResponse(
                tournamentTeamRepository.save(tournamentTeam));
    }

    @Transactional(readOnly = true)
    public List<TournamentTeamResponse> getTeams(
            Long tournamentId) {

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy giải đấu");
        }

        return tournamentTeamRepository
                .findAllByTournamentId(tournamentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void unregisterTeam(
            Long tournamentId,
            Long teamId) {

        Tournament tournament =
                tournamentRepository.findById(tournamentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy giải đấu"));

        Team team =
                teamRepository.findById(teamId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy đội"));

        User currentUser =
                currentUserService.getCurrentUser();

        boolean isTeamOwner =
                team.getOwnerUserId()
                        .equals(currentUser.getId());

        boolean isTournamentOwner =
                tournament.getCreatedBy()
                        .equals(currentUser.getId());

        if (!isTeamOwner
                && !isTournamentOwner
                && !isAdmin()) {

            throw new AccessDeniedException(
                    "Bạn không có quyền hủy đăng ký");
        }

        TournamentTeam tournamentTeam =
                tournamentTeamRepository
                        .findByTournamentIdAndTeamId(
                                tournamentId,
                                teamId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Đội chưa đăng ký giải đấu"));

        tournamentTeamRepository.delete(tournamentTeam);
    }

    private void checkRegisterPermission(Team team) {

        checkPermission(Permissions.REGISTER_TOURNAMENT);

        User currentUser =
                currentUserService.getCurrentUser();

        if (!team.getOwnerUserId()
                .equals(currentUser.getId())
                && !isAdmin()) {

            throw new AccessDeniedException(
                    "Chỉ đội trưởng mới có thể đăng ký giải đấu");
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

    private TournamentTeamResponse toResponse(
            TournamentTeam tournamentTeam) {

        return TournamentTeamResponse.builder()
                .id(tournamentTeam.getId())
                .tournamentId(
                        tournamentTeam.getTournamentId())
                .teamId(tournamentTeam.getTeamId())
                .registrationStatus(
                        tournamentTeam.getRegistrationStatus())
                .seedNumber(tournamentTeam.getSeedNumber())
                .registeredAt(
                        tournamentTeam.getRegisteredAt())
                .build();
    }
}
