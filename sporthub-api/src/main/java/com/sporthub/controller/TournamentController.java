package com.sporthub.controller;

import com.sporthub.dto.request.CreateTournamentRequest;
import com.sporthub.dto.request.UpdateTournamentRequest;
import com.sporthub.dto.request.UpdateTournamentStatusRequest;
import com.sporthub.dto.response.TournamentResponse;
import com.sporthub.service.TournamentService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class TournamentController {
    TournamentService tournamentService;

    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody CreateTournamentRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(tournamentService.createTournament(request));
    }

    @GetMapping("/{tournamentId}")
    public ResponseEntity<TournamentResponse> getTournament(
            @PathVariable Long tournamentId) {

        return ResponseEntity.ok(
                tournamentService.getTournament(tournamentId));
    }

    @PutMapping("/{tournamentId}")
    public ResponseEntity<TournamentResponse> updateTournament(
            @PathVariable Long tournamentId,
            @Valid @RequestBody UpdateTournamentRequest request) {

        return ResponseEntity.ok(
                tournamentService.updateTournament(
                        tournamentId, request));
    }

    @PutMapping("/{tournamentId}/status")
    public ResponseEntity<TournamentResponse> updateStatus(
            @PathVariable Long tournamentId,
            @Valid @RequestBody UpdateTournamentStatusRequest request) {

        return ResponseEntity.ok(
                tournamentService.updateStatus(
                        tournamentId, request));
    }
}
