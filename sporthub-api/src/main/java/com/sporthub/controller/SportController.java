package com.sporthub.controller;

import com.sporthub.dto.response.SportResponse;
import com.sporthub.service.SportService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
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
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size phải lớn hơn hoặc bằng 1") @Max(value = 100, message = "Size không được vượt quá 100") int size) {

        return ResponseEntity.ok(sportService.getAllSports(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SportResponse> getSportById(@PathVariable @Positive(message = "ID phải lớn hơn 0") Long id) {
        SportResponse response = sportService.getSportById(id);
        return ResponseEntity.ok(response);
    }
}