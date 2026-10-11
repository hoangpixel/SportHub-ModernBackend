package com.sporthub.entity;

import com.sporthub.enums.TeamMemberStatus;
import com.sporthub.enums.TeamRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long teamId;

    Long userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    TeamRole teamRole = TeamRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    TeamMemberStatus status = TeamMemberStatus.ACTIVE;

    LocalDateTime joinedAt;
}
