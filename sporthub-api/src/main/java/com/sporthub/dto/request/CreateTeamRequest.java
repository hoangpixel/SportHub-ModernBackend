package com.sporthub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateTeamRequest {
    @NotNull
    Long sportId;

    @NotBlank
    @Size(max = 100)
    String name;

    @Size(max = 500)
    String description;

    String logoUrl;
}
