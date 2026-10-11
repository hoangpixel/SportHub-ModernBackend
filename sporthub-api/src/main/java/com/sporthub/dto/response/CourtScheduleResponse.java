package com.sporthub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class CourtScheduleResponse {

    private Long id;

    private Long courtId;

    private DayOfWeek dayOfWeek;

    private LocalTime openTime;

    private LocalTime closeTime;

    private Boolean available;
}
