package com.sporthub.controller;

import com.sporthub.dto.response.CourtAvailabilityResponse;
import com.sporthub.service.CourtAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtAvailabilityController {
    private final CourtAvailabilityService availability;

    @GetMapping("/{courtId}/availability")
    public CourtAvailabilityResponse get(@PathVariable Long courtId, @RequestParam LocalDate date) {
        return availability.get(courtId, date);
    }
}
