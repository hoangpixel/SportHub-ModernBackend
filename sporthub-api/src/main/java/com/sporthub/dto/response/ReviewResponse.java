package com.sporthub.dto.response;

import com.sporthub.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long userId;
    private Long courtId;
    private Long bookingId;
    private Integer rating;
    private String comment;
    private ReviewStatus status;
    private LocalDateTime createdAt;
}