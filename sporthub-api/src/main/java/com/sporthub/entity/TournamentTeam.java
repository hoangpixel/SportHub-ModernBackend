package com.sporthub.entity;

import com.sporthub.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TournamentTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long tournamentId;

    Long teamId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    RegistrationStatus registrationStatus = RegistrationStatus.PENDING;

    Integer seedNumber;

    LocalDateTime registeredAt;
}
