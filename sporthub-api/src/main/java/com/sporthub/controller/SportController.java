package com.sporthub.controller;

import com.sporthub.dto.response.SportResponse;
import com.sporthub.service.SportService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
@Validated
public class SportController {

    private final SportService sportService;

    @GetMapping
    public ResponseEntity<Page<SportResponse>> getAllSports(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        return ResponseEntity.ok(sportService.getAllSports(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SportResponse> getSportById(@PathVariable Long id) {
        SportResponse response = sportService.getSportById(id);
        return ResponseEntity.ok(response);
    }
}