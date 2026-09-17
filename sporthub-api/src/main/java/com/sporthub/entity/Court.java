package com.sporthub.entity;

import com.sporthub.enums.CourtStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "courts",
    indexes = {
        @Index(name = "idx_court_name", columnList = "name"),
        @Index(name = "idx_court_venue_id", columnList = "venue_id"),
        @Index(name = "idx_court_sport_id", columnList = "sport_id"),
        @Index(name = "idx_court_price", columnList = "price_per_hour")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourtStatus status = CourtStatus.ACTIVE;

    @OneToMany(
        mappedBy = "court",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<CourtSchedule> schedules = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = CourtStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
