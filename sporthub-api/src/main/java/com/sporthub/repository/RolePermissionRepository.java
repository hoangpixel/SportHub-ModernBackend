package com.sporthub.repository;

import com.sporthub.entity.RolePermission;
import com.sporthub.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRole_Id(Long roleId);

    boolean existsByRole_IdAndPermission_Id(
            Long roleId,
            Long permissionId
    );
}