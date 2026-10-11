package com.sporthub.dto.response;

import com.sporthub.enums.TeamStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeamDetailResponse {
    Long id;

    Long ownerUserId;

    Long sportId;

    String name;

    String description;

    String logoUrl;

    TeamStatus status;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    List<TeamMemberResponse> members;
}
