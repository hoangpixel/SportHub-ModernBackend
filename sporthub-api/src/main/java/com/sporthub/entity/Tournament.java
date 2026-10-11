package com.sporthub.entity;

import com.sporthub.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Enumerated(EnumType.STRING)
    @Builder.Default
    TournamentStatus status = TournamentStatus.UPCOMING;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
