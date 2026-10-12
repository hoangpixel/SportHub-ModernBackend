package com.sporthub.service;

import com.sporthub.constant.Permissions;
import com.sporthub.constant.Roles;
import com.sporthub.dto.request.UpdateRolePermissionsRequest;
import com.sporthub.dto.response.AdminPermissionResponse;
import com.sporthub.dto.response.AdminRoleResponse;
import com.sporthub.entity.Permission;
import com.sporthub.entity.Role;
import com.sporthub.entity.RolePermission;
import com.sporthub.entity.RolePermissionId;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.PermissionRepository;
import com.sporthub.repository.RolePermissionRepository;
import com.sporthub.repository.RoleRepository;
import com.sporthub.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public List<AdminRoleResponse> getRoles() {
        return roleRepository.findAll().stream().map(this::toRoleResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminPermissionResponse> getPermissions() {
        List<AdminPermissionResponse> result = new ArrayList<>();

        for (String code : Permissions.ALL) {
            Permission permission = permissionRepository.findByCode(code).orElseThrow(() -> new IllegalStateException("Permission chưa được seed: " + code));
            result.add(new AdminPermissionResponse(permission.getId(), permission.getCode(), permission.getDescription()));
        }

        return result;
    }

    @Transactional
    public AdminRoleResponse updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role có id = " + roleId));

        if (Roles.ADMIN.equals(role.getName())) {
            throw new IllegalArgumentException("Không thể thay đổi quyền của ADMIN");
        }

        Set<String> uniqueCodes = new LinkedHashSet<>(request.getPermissionCodes());
        List<Permission> permissions = new ArrayList<>();

        for (String code : uniqueCodes) {
            if (!Permissions.ALL.contains(code)) {
                throw new IllegalArgumentException("Permission không hợp lệ: " + code);
            }

            Permission permission = permissionRepository.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy permission: " + code));
            permissions.add(permission);
        }

        List<RolePermission> oldPermissions = rolePermissionRepository.findByRole_Id(roleId);
        rolePermissionRepository.deleteAll(oldPermissions);

        for (Permission permission : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setId(new RolePermissionId(role.getId(), permission.getId()));
            rolePermission.setRole(role);
            rolePermission.setPermission(permission);
            rolePermissionRepository.save(rolePermission);
        }

        return toRoleResponse(role);
    }

    private AdminRoleResponse toRoleResponse(Role role) {
        List<String> permissions;

        if (Roles.ADMIN.equals(role.getName())) {
            permissions = Permissions.ALL;
        } else {
            permissions = rolePermissionRepository.findByRole_Id(role.getId()).stream().map(RolePermission::getPermission).map(Permission::getCode).toList();
        }

        long userCount = userRoleRepository.countByRole_Id(role.getId());

        return new AdminRoleResponse(role.getId(), role.getName(), userCount, permissions);
    }
}