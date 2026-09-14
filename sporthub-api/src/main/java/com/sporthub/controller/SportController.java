package com.sporthub.controller;

import com.sporthub.dto.response.SportResponse;
import com.sporthub.service.SportService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportController {

    private final SportService sportService;

    // =========================
    // GET ALL SPORTS
    // =========================
        @GetMapping
        public ResponseEntity<Page<SportResponse>> getAllSports(

                @RequestParam(defaultValue = "")
                String keyword,

                @RequestParam(defaultValue = "0")
                int page,

                @RequestParam(defaultValue = "10")
                int size) {

        return ResponseEntity.ok(
                sportService.getAllSports(
                        keyword,
                        page,
                        size
                )
        );
        }

    // =========================
    // GET SPORT BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<SportResponse>
    getSportById(
            @PathVariable Long id) {

        SportResponse response =
                sportService.getSportById(id);

        return ResponseEntity.ok(response);
    }
}