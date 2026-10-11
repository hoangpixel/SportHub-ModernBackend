package com.sporthub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class CourtScheduleRequest {

    @NotNull(message = "Ngày trong tuần không được để trống")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Giờ mở cửa không được để trống")
    private LocalTime openTime;

    @NotNull(message = "Giờ đóng cửa không được để trống")
    private LocalTime closeTime;

    private Boolean available = true;
}
