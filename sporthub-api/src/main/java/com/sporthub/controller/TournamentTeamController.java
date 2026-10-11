package com.sporthub.controller;

import com.sporthub.dto.request.RegisterTournamentTeamRequest;
import com.sporthub.dto.response.TournamentTeamResponse;
import com.sporthub.service.TournamentTeamService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/teams")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class TournamentTeamController {

    TournamentTeamService tournamentTeamService;

    @PostMapping
    public ResponseEntity<TournamentTeamResponse> registerTeam(
            @PathVariable Long tournamentId,
            @Valid @RequestBody RegisterTournamentTeamRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(tournamentTeamService.registerTeam(
                        tournamentId,
                        request));
    }

    @GetMapping
    public ResponseEntity<List<TournamentTeamResponse>> getTeams(
            @PathVariable Long tournamentId) {

        return ResponseEntity.ok(
                tournamentTeamService.getTeams(tournamentId));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> unregisterTeam(
            @PathVariable Long tournamentId,
            @PathVariable Long teamId) {

        tournamentTeamService.unregisterTeam(
                tournamentId,
                teamId);

        return ResponseEntity.noContent().build();
    }
}
