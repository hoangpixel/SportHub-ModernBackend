package com.sporthub.seeder;

import com.sporthub.constant.Permissions;
import com.sporthub.constant.Roles;
import com.sporthub.entity.Permission;
import com.sporthub.entity.Role;
import com.sporthub.entity.RolePermission;
import com.sporthub.entity.RolePermissionId;
import com.sporthub.repository.PermissionRepository;
import com.sporthub.repository.RolePermissionRepository;
import com.sporthub.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PermissionSeeder {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public void seed() {

        // 1. Tạo toàn bộ permission
        for (String permissionCode : Permissions.ALL) {
            createPermissionIfNotExists(permissionCode);
        }

        // 2. Lấy role
        Role userRole = roleRepository.findByName(Roles.USER)
                .orElseThrow();

        Role ownerRole = roleRepository.findByName(Roles.OWNER)
                .orElseThrow();

        Role adminRole = roleRepository.findByName(Roles.ADMIN)
                .orElseThrow();

        // 3. USER permissions
        List<String> userPermissions = List.of(
                Permissions.CREATE_BOOKING,
                Permissions.VIEW_OWN_BOOKINGS,
                Permissions.CREATE_TEAM,
                Permissions.REGISTER_TOURNAMENT,
                Permissions.CREATE_REVIEW
        );

        // 4. OWNER = USER + OWNER permissions
        List<String> ownerPermissions = List.of(
                Permissions.CREATE_BOOKING,
                Permissions.VIEW_OWN_BOOKINGS,
                Permissions.CREATE_TEAM,
                Permissions.REGISTER_TOURNAMENT,
                Permissions.CREATE_REVIEW,

                Permissions.CREATE_VENUE,
                Permissions.UPDATE_OWN_VENUE,
                Permissions.MANAGE_OWN_COURTS,
                Permissions.VIEW_OWNER_BOOKINGS,
                Permissions.VIEW_OWNER_DASHBOARD
        );

        // 5. ADMIN có tất cả
        List<String> adminPermissions = Permissions.ALL;

        assignPermissions(userRole, userPermissions);
        assignPermissions(ownerRole, ownerPermissions);
        assignPermissions(adminRole, adminPermissions);
    }

    private void createPermissionIfNotExists(String code) {

        if (permissionRepository.existsByCode(code)) {
            return;
        }

        Permission permission = new Permission();
        permission.setCode(code);
        permission.setDescription("Permission: " + code);

        permissionRepository.save(permission);
    }

    private void assignPermissions(
            Role role,
            List<String> permissionCodes) {

        for (String code : permissionCodes) {

            Permission permission =
                    permissionRepository.findByCode(code)
                            .orElseThrow();

            boolean exists =
                    rolePermissionRepository
                            .existsByRole_IdAndPermission_Id(
                                    role.getId(),
                                    permission.getId()
                            );

            if (exists) {
                continue;
            }

            RolePermission rolePermission =
                    new RolePermission();

            rolePermission.setId(
                    new RolePermissionId(
                            role.getId(),
                            permission.getId()
                    )
            );

            rolePermission.setRole(role);
            rolePermission.setPermission(permission);

            rolePermissionRepository.save(rolePermission);
        }
    }
}