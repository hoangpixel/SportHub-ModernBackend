package com.sporthub.dto.response;

import com.sporthub.enums.TeamStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TeamResponse {
    Long id;

    Long ownerUserId;

    Long sportId;

    String name;

    String description;

    String logoUrl;

    TeamStatus status;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
