package com.sporthub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTournamentRequest {
    @NotBlank
    @Size(max = 100)
    String name;

    @Size(max = 500)
    String description;

    @NotNull
    LocalDate startDate;

    @NotNull
    LocalDate endDate;

    @NotNull
    LocalDate registrationStart;

    @NotNull
    LocalDate registrationEnd;

    @NotNull
    @Min(2)
    Integer maxTeams;
}
