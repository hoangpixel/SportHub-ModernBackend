package com.sporthub.controller;

import com.sporthub.dto.request.SportRequest;
import com.sporthub.dto.response.SportResponse;
import com.sporthub.service.SportService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/sports")
@RequiredArgsConstructor
public class AdminSportController {

    private final SportService sportService;

    // =========================
    // CREATE SPORT
    // =========================
    @PostMapping
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).CREATE_SPORT)"
    )
    public ResponseEntity<SportResponse>
    createSport(
            @Valid
            @RequestBody SportRequest request) {

        SportResponse response =
                sportService.createSport(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // UPDATE SPORT
    // =========================
    @PutMapping("/{id}")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_SPORT)"
    )
    public ResponseEntity<SportResponse>
    updateSport(
            @PathVariable Long id,
            @Valid
            @RequestBody SportRequest request) {

        SportResponse response =
                sportService.updateSport(
                        id,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping(
        value = "/{id}/icon",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
        )
        @PreAuthorize(
                "hasAuthority("
                + "T(com.sporthub.constant.Permissions)"
                + ".UPDATE_SPORT)"
        )
        public ResponseEntity<SportResponse> updateIcon(
                @PathVariable Long id,
                @RequestPart("file")
                MultipartFile file) {

        return ResponseEntity.ok(
                sportService.updateIcon(
                        id,
                        file
                )
        );
        }

    // =========================
    // DELETE SPORT
    // =========================
    @DeleteMapping("/{id}")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).DELETE_SPORT)"
    )
    public ResponseEntity<Map<String, String>>
    deleteSport(
            @PathVariable Long id) {

        sportService.deleteSport(id);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Xóa môn thể thao thành công"
                )
        );
    }
}