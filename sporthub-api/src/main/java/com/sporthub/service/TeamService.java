package com.sporthub.service;

import com.sporthub.constant.Roles;
import com.sporthub.dto.request.AddTeamMemberRequest;
import com.sporthub.dto.request.CreateTeamRequest;
import com.sporthub.dto.request.UpdateTeamRequest;
import com.sporthub.dto.response.TeamDetailResponse;
import com.sporthub.dto.response.TeamMemberResponse;
import com.sporthub.dto.response.TeamResponse;
import com.sporthub.entity.Team;
import com.sporthub.entity.TeamMember;
import com.sporthub.entity.User;
import com.sporthub.enums.TeamRole;
import com.sporthub.enums.TeamStatus;
import com.sporthub.enums.UserStatus;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.SportRepository;
import com.sporthub.repository.TeamMemberRepository;
import com.sporthub.repository.TeamRepository;
import com.sporthub.repository.UserRepository;
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
public class TeamService {
    TeamRepository teamRepository;
    TeamMemberRepository teamMemberRepository;
    UserRepository userRepository;
    SportRepository sportRepository;
    CurrentUserService currentUserService;

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        if (!sportRepository.existsById(request.getSportId())) {
            throw new ResourceNotFoundException("Không tìm thấy bộ môn");
        }

        Team team = Team.builder()
                .ownerUserId(currentUser.getId())
                .sportId(request.getSportId())
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Team savedTeam = teamRepository.save(team);

        TeamMember captain = TeamMember.builder()
                .teamId(savedTeam.getId())
                .userId(currentUser.getId())
                .teamRole(TeamRole.CAPTAIN)
                .joinedAt(LocalDateTime.now())
                .build();

        teamMemberRepository.save(captain);

        return toTeamResponse(savedTeam);
    }

    @Transactional
    public TeamResponse updateTeam(
            Long teamId,
            UpdateTeamRequest request) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy đội"
                        ));

        User currentUser = currentUserService.getCurrentUser();

        checkTeamManager(team, currentUser.getId());

        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setLogoUrl(request.getLogoUrl());
        team.setUpdatedAt(LocalDateTime.now());

        Team updatedTeam = teamRepository.save(team);

        return toTeamResponse(updatedTeam);
    }

    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetail(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy đội"
                        ));

        List<TeamMemberResponse> members =
                teamMemberRepository.findAllByTeamId(teamId)
                        .stream()
                        .map(this::toTeamMemberResponse)
                        .toList();

        return TeamDetailResponse.builder()
                .id(team.getId())
                .ownerUserId(team.getOwnerUserId())
                .sportId(team.getSportId())
                .name(team.getName())
                .description(team.getDescription())
                .logoUrl(team.getLogoUrl())
                .status(team.getStatus())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .members(members)
                .build();
    }

    @Transactional
    public TeamDetailResponse addMember(
            Long teamId,
            AddTeamMemberRequest request) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy đội"
                        ));

        User currentUser = currentUserService.getCurrentUser();

        checkTeamManager(team, currentUser.getId());

        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Đội chưa được kích hoạt"
            );
        }

        User user = userRepository
                .findByUsernameOrEmail(
                        request.getKeyword(),
                        request.getKeyword()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy tài khoản"
                        ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Tài khoản người dùng chưa hoạt động"
            );
        }

        if (teamMemberRepository.existsByTeamIdAndUserId(
                teamId,
                user.getId())) {

            throw new IllegalStateException(
                    "Người dùng đã có trong đội"
            );
        }

        TeamMember member = TeamMember.builder()
                .teamId(teamId)
                .userId(user.getId())
                .joinedAt(LocalDateTime.now())
                .build();

        teamMemberRepository.save(member);

        return getTeamDetail(teamId);
    }

    @Transactional
    public void removeMember(
            Long teamId,
            Long userId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy đội"
                        ));

        User currentUser = currentUserService.getCurrentUser();

        checkTeamManager(team, currentUser.getId());

        if (team.getOwnerUserId().equals(userId)) {
            throw new IllegalStateException(
                    "Không thể xóa đội trưởng"
            );
        }

        TeamMember member =
                teamMemberRepository
                        .findByTeamIdAndUserId(teamId, userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Thành viên không tồn tại trong đội"
                                ));

        teamMemberRepository.delete(member);
    }

    @Transactional
    public void leaveTeam(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy đội"
                        ));

        User currentUser = currentUserService.getCurrentUser();

        if (team.getOwnerUserId().equals(currentUser.getId())) {
            throw new IllegalStateException(
                    "Đội trưởng không thể rời đội"
            );
        }

        TeamMember member =
                teamMemberRepository
                        .findByTeamIdAndUserId(
                                teamId,
                                currentUser.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Bạn không phải thành viên của đội"
                                ));

        teamMemberRepository.delete(member);
    }

    private void checkTeamManager(
            Team team,
            Long userId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_" + Roles.ADMIN));

        if (isAdmin) {
            return;
        }

        if (!team.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedException(
                    "Bạn không có quyền thực hiện thao tác này"
            );
        }
    }

    private TeamResponse toTeamResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .ownerUserId(team.getOwnerUserId())
                .sportId(team.getSportId())
                .name(team.getName())
                .description(team.getDescription())
                .logoUrl(team.getLogoUrl())
                .status(team.getStatus())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private TeamMemberResponse toTeamMemberResponse(
            TeamMember member) {

        User user = userRepository.findById(member.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy user"
                        ));

        return TeamMemberResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .teamRole(member.getTeamRole())
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
