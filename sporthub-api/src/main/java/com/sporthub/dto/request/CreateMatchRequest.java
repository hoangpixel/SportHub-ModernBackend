package com.sporthub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateMatchRequest {

    @NotNull
    Long tournamentId;

    @NotNull
    @Min(1)
    Integer roundNumber;

    @NotNull
    Long courtId;

    @NotNull
    Long team1Id;

    @NotNull
    Long team2Id;

    @NotNull
    LocalDate scheduledDate;

    @NotNull
    LocalTime startTime;

    @NotNull
    LocalTime endTime;
}
