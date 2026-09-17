package com.sporthub.controller;

import com.sporthub.dto.response.VenueResponse;
import com.sporthub.service.VenueService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Venues", description = "Public venue search and detail")
@io.swagger.v3.oas.annotations.security.SecurityRequirements
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public ResponseEntity<Page<VenueResponse>> searchVenues(

            @RequestParam(defaultValue = "")
            String keyword,

            @RequestParam(required = false)
            String city,

            @RequestParam(required = false)
            String district,

            @RequestParam(required = false)
            Long ownerId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String direction) {

        return ResponseEntity.ok(
                venueService.searchVenues(
                        keyword, city, district, ownerId,
                        page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueResponse> getVenueById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                venueService.getVenueById(id));
    }
}
