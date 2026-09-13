package com.sporthub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "permissions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_permissions_code",
            columnNames = "code"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @OneToMany(
        mappedBy = "permission",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<RolePermission> rolePermissions = new ArrayList<>();
}