package com.sporthub.dto.response;

import com.sporthub.enums.RegistrationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TournamentTeamResponse {
    Long id;
    Long tournamentId;
    Long teamId;
    RegistrationStatus registrationStatus;
    Integer seedNumber;
    LocalDateTime registeredAt;
}
