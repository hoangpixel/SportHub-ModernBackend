package com.sporthub.controller;

import com.sporthub.dto.request.CreateMatchRequest;
import com.sporthub.dto.request.UpdateMatchResultRequest;
import com.sporthub.dto.response.MatchResponse;
import com.sporthub.service.MatchService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MatchController {

    MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(
            @Valid @RequestBody CreateMatchRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(matchService.createMatch(request));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResponse> getMatch(
            @PathVariable Long matchId) {

        return ResponseEntity.ok(
                matchService.getMatch(matchId));
    }

    @PutMapping("/{matchId}/result")
    public ResponseEntity<MatchResponse> updateResult(
            @PathVariable Long matchId,
            @Valid @RequestBody UpdateMatchResultRequest request) {

        return ResponseEntity.ok(
                matchService.updateResult(
                        matchId,
                        request));
    }

    @PutMapping("/{matchId}/cancel")
    public ResponseEntity<MatchResponse> cancelMatch(
            @PathVariable Long matchId) {

        return ResponseEntity.ok(
                matchService.cancelMatch(matchId));
    }
}
