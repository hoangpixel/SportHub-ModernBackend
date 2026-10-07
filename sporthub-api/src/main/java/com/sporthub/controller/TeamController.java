package com.sporthub.controller;

import com.sporthub.dto.request.AddTeamMemberRequest;
import com.sporthub.dto.request.CreateTeamRequest;
import com.sporthub.dto.request.UpdateTeamRequest;
import com.sporthub.dto.response.TeamDetailResponse;
import com.sporthub.dto.response.TeamResponse;
import com.sporthub.service.TeamService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class TeamController {

    TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamService.createTeam(request));
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request) {

        return ResponseEntity.ok(
                teamService.updateTeam(teamId, request)
        );
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> getTeamDetail(
            @PathVariable Long teamId) {

        return ResponseEntity.ok(
                teamService.getTeamDetail(teamId)
        );
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamDetailResponse> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddTeamMemberRequest request) {

        return ResponseEntity.ok(
                teamService.addMember(teamId, request)
        );
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId) {

        teamService.removeMember(teamId, userId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{teamId}/members/me")
    public ResponseEntity<Void> leaveTeam(
            @PathVariable Long teamId) {

        teamService.leaveTeam(teamId);

        return ResponseEntity.noContent().build();
    }
}
