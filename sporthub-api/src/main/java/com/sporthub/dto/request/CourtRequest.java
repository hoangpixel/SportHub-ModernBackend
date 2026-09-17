package com.sporthub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class CourtRequest {

    @NotBlank(message = "Tên sân không được để trống")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Sport ID không được để trống")
    private Long sportId;

    @NotNull(message = "Giá mỗi giờ không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal pricePerHour;

    private LocalTime openTime;

    private LocalTime closeTime;
}
