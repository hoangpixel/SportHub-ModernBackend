package com.sporthub.entity;

import com.sporthub.enums.TeamStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long ownerUserId;

    Long sportId;

    String name;

    String description;

    String logoUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    TeamStatus status = TeamStatus.ACTIVE;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
