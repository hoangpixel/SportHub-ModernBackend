package com.sporthub.dto.response;

import com.sporthub.enums.CourtStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class CourtResponse {

    private Long id;

    private String name;

    private String description;

    private Long venueId;

    private String venueName;

    private Long sportId;

    private String sportName;

    private BigDecimal pricePerHour;

    private LocalTime openTime;

    private LocalTime closeTime;

    private CourtStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
