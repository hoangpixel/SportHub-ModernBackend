package com.sporthub.dto.request;

import com.sporthub.enums.TournamentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTournamentStatusRequest {
    @NotNull
    TournamentStatus status;
}
