package com.sporthub.dto.response;

import com.sporthub.enums.TeamMemberStatus;
import com.sporthub.enums.TeamRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeamMemberResponse {
    Long id;

    String username;

    String email;

    TeamRole teamRole;

    TeamMemberStatus status;

    LocalDateTime joinedAt;
}
