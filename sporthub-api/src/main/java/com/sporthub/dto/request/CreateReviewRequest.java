package com.sporthub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateReviewRequest {

    @NotNull(message = "Mã booking không được để trống")
    @Positive
    private Long bookingId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm tối thiểu là 1")
    @Max(value = 5, message = "Điểm tối đa là 5")
    private Integer rating;

    @Size(max = 1000, message = "Nội dung đánh giá tối đa 1000 ký tự")
    private String comment;
}