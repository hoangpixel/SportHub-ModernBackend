package com.sporthub.dto.response;

import com.sporthub.enums.MatchStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchResponse {

    Long id;
    Long tournamentId;
    Integer roundNumber;
    Long courtId;
    Long team1Id;
    Long team2Id;
    LocalDate scheduledDate;
    LocalTime startTime;
    LocalTime endTime;
    Integer team1Score;
    Integer team2Score;
    Long winnerTeamId;
    MatchStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
