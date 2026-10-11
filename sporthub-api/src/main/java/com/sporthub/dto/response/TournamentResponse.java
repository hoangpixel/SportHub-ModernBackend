package com.sporthub.dto.response;

import com.sporthub.enums.TournamentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TournamentResponse {
    Long id;
    Long sportId;
    Long venueId;
    Long createdBy;
    String name;
    String description;
    LocalDate startDate;
    LocalDate endDate;
    LocalDate registrationStart;
    LocalDate registrationEnd;
    Integer maxTeams;
    TournamentStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
