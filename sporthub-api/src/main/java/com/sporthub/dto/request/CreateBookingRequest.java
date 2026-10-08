package com.sporthub.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CreateBookingRequest {

    @NotNull(message = "Vui lòng chọn sân")
    @Positive(message = "Mã sân phải lớn hơn 0")
    private Long courtId;

    @NotNull(message = "Ngày đặt sân không được để trống")
    private LocalDate bookingDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;
}