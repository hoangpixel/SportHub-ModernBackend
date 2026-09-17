package com.sporthub.entity;

import com.sporthub.enums.VenueStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "venues",
    indexes = {
        @Index(name = "idx_venue_name", columnList = "name"),
        @Index(name = "idx_venue_city", columnList = "city"),
        @Index(name = "idx_venue_district", columnList = "district"),
        @Index(name = "idx_venue_owner_id", columnList = "owner_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VenueStatus status = VenueStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(
        mappedBy = "venue",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Court> courts = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = VenueStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
