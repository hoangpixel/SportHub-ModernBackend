package com.sporthub.entity;

import com.sporthub.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long tournamentId;

    Integer roundNumber;

    Long courtId;

    Long team1Id;

    Long team2Id;

    LocalDate scheduledDate;

    LocalTime startTime;

    LocalTime endTime;

    Integer team1Score = 0;

    Integer team2Score = 0;

    Long winnerTeamId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    MatchStatus status = MatchStatus.SCHEDULED;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
