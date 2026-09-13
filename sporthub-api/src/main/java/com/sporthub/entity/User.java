package com.sporthub.entity;

import com.sporthub.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @OneToOne(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private UserProfile profile;

    @OneToMany(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<UserRole> userRoles = new ArrayList<>();
}