package com.sporthub.controller;

import com.sporthub.dto.response.CourtResponse;
import com.sporthub.dto.response.CourtScheduleResponse;
import com.sporthub.service.CourtScheduleService;
import com.sporthub.service.CourtService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Courts", description = "Public court search, detail and weekly schedules")
@io.swagger.v3.oas.annotations.security.SecurityRequirements
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;
    private final CourtScheduleService courtScheduleService;

    @GetMapping
    public ResponseEntity<Page<CourtResponse>> searchCourts(

            @RequestParam(defaultValue = "")
            String keyword,

            @RequestParam(required = false)
            Long sportId,

            @RequestParam(required = false)
            Long venueId,

            @RequestParam(required = false)
            BigDecimal minPrice,

            @RequestParam(required = false)
            BigDecimal maxPrice,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String direction) {

        return ResponseEntity.ok(
                courtService.searchCourts(
                        keyword, sportId, venueId,
                        minPrice, maxPrice,
                        page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourtResponse> getCourtById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                courtService.getCourtById(id));
    }

    @GetMapping("/{id}/schedules")
    public ResponseEntity<List<CourtScheduleResponse>>
    getSchedulesByCourt(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                courtScheduleService.getSchedulesByCourt(id));
    }
}
