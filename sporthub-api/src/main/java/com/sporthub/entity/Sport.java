package com.sporthub.entity;

import com.sporthub.enums.SportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports")
@Getter
@Setter
@NoArgsConstructor
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SportStatus status = SportStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = SportStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}