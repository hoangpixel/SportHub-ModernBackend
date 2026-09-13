package com.sporthub.repository;

import com.sporthub.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository
        extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        SELECT DISTINCT p
        FROM UserRole ur
        JOIN ur.role r
        JOIN r.rolePermissions rp
        JOIN rp.permission p
        WHERE ur.user.username = :username
    """)
    List<Permission> findPermissionsByUsername(
            @Param("username") String username
    );
}