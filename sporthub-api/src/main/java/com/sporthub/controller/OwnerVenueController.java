package com.sporthub.controller;

import com.sporthub.dto.request.VenueRequest;
import com.sporthub.dto.response.VenueResponse;
import com.sporthub.service.VenueService;

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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Owner venues", description = "Manage venues belonging to the authenticated owner")
@RequestMapping("/api/owner/venues")
@RequiredArgsConstructor
public class OwnerVenueController {

    private final VenueService venueService;

    @GetMapping
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).CREATE_VENUE)"
    )
    public ResponseEntity<Page<VenueResponse>> getMyVenues(
            Authentication authentication,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size) {

        return ResponseEntity.ok(
                venueService.getMyVenues(
                        authentication.getName(),
                        page, size));
    }

    @PostMapping
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).CREATE_VENUE)"
    )
    public ResponseEntity<VenueResponse> createVenue(
            Authentication authentication,
            @Valid @RequestBody VenueRequest request) {

        VenueResponse response = venueService.createVenue(
                authentication.getName(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_OWN_VENUE)"
    )
    public ResponseEntity<VenueResponse> updateVenue(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody VenueRequest request) {

        VenueResponse response = venueService.updateVenue(
                authentication.getName(), id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_OWN_VENUE)"
    )
    public ResponseEntity<Map<String, String>> deleteVenue(
            Authentication authentication,
            @PathVariable Long id) {

        venueService.deleteVenue(
                authentication.getName(), id);

        return ResponseEntity.ok(
                Map.of("message",
                       "Xóa cơ sở thành công"));
    }
}
