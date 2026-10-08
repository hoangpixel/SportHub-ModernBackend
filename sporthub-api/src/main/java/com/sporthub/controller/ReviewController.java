package com.sporthub.controller;

import com.sporthub.dto.request.CreateReviewRequest;
import com.sporthub.dto.response.ReviewResponse;
import com.sporthub.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_REVIEW')")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(request));
    }

    @GetMapping("/court/{courtId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByCourtId(
            @PathVariable Long courtId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getReviewsByCourtId(courtId, page, size));
    }

    @GetMapping("/court/{courtId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long courtId) {
        return ResponseEntity.ok(reviewService.getAverageRating(courtId));
    }
}