package com.sporthub.controller;

import com.sporthub.dto.request.CourtRequest;
import com.sporthub.dto.request.CourtScheduleRequest;
import com.sporthub.dto.response.CourtResponse;
import com.sporthub.dto.response.CourtScheduleResponse;
import com.sporthub.service.CourtScheduleService;
import com.sporthub.service.CourtService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Owner courts", description = "Manage owned courts and their weekly schedules")
@RequestMapping("/api/owner/venues/{venueId}/courts")
@RequiredArgsConstructor
public class OwnerCourtController {

    private final CourtService courtService;
    private final CourtScheduleService courtScheduleService;

    @GetMapping
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).MANAGE_OWN_COURTS)"
    )
    public ResponseEntity<Page<CourtResponse>> getCourtsByVenue(
            Authentication authentication,
            @PathVariable Long venueId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size) {

        return ResponseEntity.ok(
                courtService.getMyCourtsByVenue(
                        authentication.getName(), venueId, page, size));
    }

    @PostMapping
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).MANAGE_OWN_COURTS)"
    )
    public ResponseEntity<CourtResponse> createCourt(
            Authentication authentication,
            @PathVariable Long venueId,
            @Valid @RequestBody CourtRequest request) {

        CourtResponse response = courtService.createCourt(
                authentication.getName(),
                venueId,
                request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{courtId}")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).MANAGE_OWN_COURTS)"
    )
    public ResponseEntity<CourtResponse> updateCourt(
            Authentication authentication,
            @PathVariable Long venueId,
            @PathVariable Long courtId,
            @Valid @RequestBody CourtRequest request) {

        courtService.requireCourtInVenue(courtId, venueId);
        CourtResponse response = courtService.updateCourt(
                authentication.getName(),
                courtId,
                request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{courtId}")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).MANAGE_OWN_COURTS)"
    )
    public ResponseEntity<Map<String, String>> deleteCourt(
            Authentication authentication,
            @PathVariable Long venueId,
            @PathVariable Long courtId) {

        courtService.requireCourtInVenue(courtId, venueId);
        courtService.deleteCourt(
                authentication.getName(), courtId);

        return ResponseEntity.ok(
                Map.of("message",
                       "Xóa sân thành công"));
    }

    @PostMapping("/{courtId}/schedules")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).MANAGE_OWN_COURTS)"
    )
    public ResponseEntity<CourtScheduleResponse> createSchedule(
            Authentication authentication,
            @PathVariable Long venueId,
            @PathVariable Long courtId,
            @Valid @RequestBody CourtScheduleRequest request) {

        courtService.requireCourtInVenue(courtId, venueId);
        CourtScheduleResponse response =
                courtScheduleService.createSchedule(
                        authentication.getName(),
                        courtId,
                        request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{courtId}/schedules/{scheduleId}")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).MANAGE_OWN_COURTS)"
    )
    public ResponseEntity<CourtScheduleResponse> updateSchedule(
            Authentication authentication,
            @PathVariable Long venueId,
            @PathVariable Long courtId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody CourtScheduleRequest request) {

        courtService.requireCourtInVenue(courtId, venueId);
        courtScheduleService.requireScheduleInCourt(scheduleId, courtId);
        CourtScheduleResponse response =
                courtScheduleService.updateSchedule(
                        authentication.getName(),
                        scheduleId,
                        request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{courtId}/schedules/{scheduleId}")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).MANAGE_OWN_COURTS)"
    )
    public ResponseEntity<Map<String, String>> deleteSchedule(
            Authentication authentication,
            @PathVariable Long venueId,
            @PathVariable Long courtId,
            @PathVariable Long scheduleId) {

        courtService.requireCourtInVenue(courtId, venueId);
        courtScheduleService.requireScheduleInCourt(scheduleId, courtId);
        courtScheduleService.deleteSchedule(
                authentication.getName(), scheduleId);

        return ResponseEntity.ok(
                Map.of("message",
                       "Xóa lịch sân thành công"));
    }
}
