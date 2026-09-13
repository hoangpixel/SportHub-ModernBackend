package com.sporthub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "roles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @OneToMany(
        mappedBy = "role",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<UserRole> userRoles = new ArrayList<>();

    @OneToMany(
        mappedBy = "role",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<RolePermission> rolePermissions = new ArrayList<>();
}