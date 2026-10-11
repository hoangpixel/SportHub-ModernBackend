package com.sporthub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMatchResultRequest {

    @NotNull
    @Min(0)
    Integer team1Score;

    @NotNull
    @Min(0)
    Integer team2Score;
}
