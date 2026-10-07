package com.sporthub.service;

import com.sporthub.constant.Permissions;
import com.sporthub.constant.Roles;
import com.sporthub.dto.request.CreateTournamentRequest;
import com.sporthub.dto.request.UpdateTournamentRequest;
import com.sporthub.dto.request.UpdateTournamentStatusRequest;
import com.sporthub.dto.response.TournamentResponse;
import com.sporthub.entity.Tournament;
import com.sporthub.entity.User;
import com.sporthub.enums.TournamentStatus;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.SportRepository;
import com.sporthub.repository.TournamentRepository;
import com.sporthub.security.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class TournamentService {
    TournamentRepository tournamentRepository;
    SportRepository sportRepository;
    CurrentUserService currentUserService;

    @Transactional
    public TournamentResponse createTournament(
            CreateTournamentRequest request) {

        checkPermission(Permissions.MANAGE_TOURNAMENTS);

        if (!sportRepository.existsById(request.getSportId())) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy bộ môn");
        }

        validateDates(
                request.getStartDate(),
                request.getEndDate(),
                request.getRegistrationStart(),
                request.getRegistrationEnd());

        User currentUser = currentUserService.getCurrentUser();

        LocalDateTime now = LocalDateTime.now();

        Tournament tournament = Tournament.builder()
                .sportId(request.getSportId())
                .venueId(request.getVenueId())
                .createdBy(currentUser.getId())
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .registrationStart(request.getRegistrationStart())
                .registrationEnd(request.getRegistrationEnd())
                .maxTeams(request.getMaxTeams())
                .status(TournamentStatus.UPCOMING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(
                tournamentRepository.save(tournament));
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournament(Long tournamentId) {

        Tournament tournament = findTournament(tournamentId);

        return toResponse(tournament);
    }

    @Transactional
    public TournamentResponse updateTournament(
            Long tournamentId,
            UpdateTournamentRequest request) {

        Tournament tournament = findTournament(tournamentId);

        checkManagePermission(tournament);

        validateDates(
                request.getStartDate(),
                request.getEndDate(),
                request.getRegistrationStart(),
                request.getRegistrationEnd());

        tournament.setName(request.getName());
        tournament.setDescription(request.getDescription());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setRegistrationStart(request.getRegistrationStart());
        tournament.setRegistrationEnd(request.getRegistrationEnd());
        tournament.setMaxTeams(request.getMaxTeams());
        tournament.setUpdatedAt(LocalDateTime.now());

        return toResponse(
                tournamentRepository.save(tournament));
    }

    @Transactional
    public TournamentResponse updateStatus(
            Long tournamentId,
            UpdateTournamentStatusRequest request) {

        Tournament tournament = findTournament(tournamentId);

        checkManagePermission(tournament);

        tournament.setStatus(request.getStatus());
        tournament.setUpdatedAt(LocalDateTime.now());

        return toResponse(
                tournamentRepository.save(tournament));
    }

    private Tournament findTournament(Long tournamentId) {

        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy giải đấu"));
    }

    private void checkManagePermission(Tournament tournament) {

        if (isAdmin()) {
            return;
        }

        checkPermission(Permissions.MANAGE_TOURNAMENTS);

        User currentUser = currentUserService.getCurrentUser();

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

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationStart,
            LocalDate registrationEnd) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu giải đấu phải trước ngày kết thúc");
        }

        if (registrationStart.isAfter(registrationEnd)) {
            throw new IllegalArgumentException(
                    "Thời gian đăng ký không hợp lệ");
        }

        if (registrationEnd.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "Đăng ký phải kết thúc trước khi giải đấu bắt đầu");
        }
    }

    private TournamentResponse toResponse(
            Tournament tournament) {

        return TournamentResponse.builder()
                .id(tournament.getId())
                .sportId(tournament.getSportId())
                .venueId(tournament.getVenueId())
                .createdBy(tournament.getCreatedBy())
                .name(tournament.getName())
                .description(tournament.getDescription())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .registrationStart(
                        tournament.getRegistrationStart())
                .registrationEnd(
                        tournament.getRegistrationEnd())
                .maxTeams(tournament.getMaxTeams())
                .status(tournament.getStatus())
                .createdAt(tournament.getCreatedAt())
                .updatedAt(tournament.getUpdatedAt())
                .build();
    }
}
