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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private UserRoleRepository userRoleRepository;

    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        accessControlService = new AccessControlService(roleRepository, permissionRepository, rolePermissionRepository, userRoleRepository);
    }

    private Role createRole(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }

    private Permission createPermission(Long id, String code) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(code);
        permission.setDescription("Permission: " + code);
        return permission;
    }

    private RolePermission createRolePermission(Role role, Permission permission) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setId(new RolePermissionId(role.getId(), permission.getId()));
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        return rolePermission;
    }

    @Test
    void get_roles_success() {
        Role userRole = createRole(1L, Roles.USER);
        Role adminRole = createRole(3L, Roles.ADMIN);
        Permission permission = createPermission(1L, Permissions.CREATE_BOOKING);
        RolePermission rolePermission = createRolePermission(userRole, permission);

        when(roleRepository.findAll()).thenReturn(List.of(userRole, adminRole));
        when(rolePermissionRepository.findByRole_Id(1L)).thenReturn(List.of(rolePermission));
        when(userRoleRepository.countByRole_Id(1L)).thenReturn(5L);
        when(userRoleRepository.countByRole_Id(3L)).thenReturn(1L);

        List<AdminRoleResponse> result = accessControlService.getRoles();

        assertEquals(2, result.size());
        assertEquals(Roles.USER, result.get(0).getName());
        assertEquals(5L, result.get(0).getUserCount());
        assertTrue(result.get(0).getPermissions().contains(Permissions.CREATE_BOOKING));
        assertEquals(Roles.ADMIN, result.get(1).getName());
        assertEquals(Permissions.ALL.size(), result.get(1).getPermissions().size());
    }

    @Test
    void get_permissions_success() {
        long id = 1L;

        for (String code : Permissions.ALL) {
            Permission permission = createPermission(id++, code);
            when(permissionRepository.findByCode(code)).thenReturn(Optional.of(permission));
        }

        List<AdminPermissionResponse> result = accessControlService.getPermissions();

        assertEquals(Permissions.ALL.size(), result.size());
        assertEquals(Permissions.ALL.get(0), result.get(0).getCode());

        for (String code : Permissions.ALL) {
            verify(permissionRepository).findByCode(code);
        }
    }

    @Test
    void update_role_permissions_success() {
        Role ownerRole = createRole(2L, Roles.OWNER);
        Permission createBooking = createPermission(1L, Permissions.CREATE_BOOKING);
        Permission viewBookings = createPermission(2L, Permissions.VIEW_OWN_BOOKINGS);
        Permission oldPermission = createPermission(3L, Permissions.CREATE_TEAM);

        RolePermission oldRolePermission = createRolePermission(ownerRole, oldPermission);
        RolePermission newRolePermission1 = createRolePermission(ownerRole, createBooking);
        RolePermission newRolePermission2 = createRolePermission(ownerRole, viewBookings);

        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
        request.setPermissionCodes(List.of(Permissions.CREATE_BOOKING, Permissions.VIEW_OWN_BOOKINGS));

        when(roleRepository.findById(2L)).thenReturn(Optional.of(ownerRole));
        when(permissionRepository.findByCode(Permissions.CREATE_BOOKING)).thenReturn(Optional.of(createBooking));
        when(permissionRepository.findByCode(Permissions.VIEW_OWN_BOOKINGS)).thenReturn(Optional.of(viewBookings));
        when(rolePermissionRepository.findByRole_Id(2L)).thenReturn(List.of(oldRolePermission), List.of(newRolePermission1, newRolePermission2));
        when(userRoleRepository.countByRole_Id(2L)).thenReturn(3L);

        AdminRoleResponse response = accessControlService.updateRolePermissions(2L, request);

        assertEquals(Roles.OWNER, response.getName());
        assertEquals(3L, response.getUserCount());
        assertEquals(2, response.getPermissions().size());
        assertTrue(response.getPermissions().contains(Permissions.CREATE_BOOKING));
        assertTrue(response.getPermissions().contains(Permissions.VIEW_OWN_BOOKINGS));

        verify(rolePermissionRepository).deleteAll(List.of(oldRolePermission));
        verify(rolePermissionRepository, times(2)).save(any(RolePermission.class));
    }

    @Test
    void update_role_permissions_duplicate_codes_should_save_once() {
        Role ownerRole = createRole(2L, Roles.OWNER);
        Permission permission = createPermission(1L, Permissions.CREATE_BOOKING);
        RolePermission rolePermission = createRolePermission(ownerRole, permission);

        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
        request.setPermissionCodes(List.of(Permissions.CREATE_BOOKING, Permissions.CREATE_BOOKING));

        when(roleRepository.findById(2L)).thenReturn(Optional.of(ownerRole));
        when(permissionRepository.findByCode(Permissions.CREATE_BOOKING)).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.findByRole_Id(2L)).thenReturn(List.of(), List.of(rolePermission));
        when(userRoleRepository.countByRole_Id(2L)).thenReturn(1L);

        AdminRoleResponse response = accessControlService.updateRolePermissions(2L, request);

        assertEquals(1, response.getPermissions().size());
        verify(permissionRepository, times(1)).findByCode(Permissions.CREATE_BOOKING);
        verify(rolePermissionRepository, times(1)).save(any(RolePermission.class));
    }

    @Test
    void update_role_permissions_invalid_permission_shouldThrowException() {
        Role ownerRole = createRole(2L, Roles.OWNER);

        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
        request.setPermissionCodes(List.of("HACK_THE_WORLD"));

        when(roleRepository.findById(2L)).thenReturn(Optional.of(ownerRole));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accessControlService.updateRolePermissions(2L, request));

        assertEquals("Permission không hợp lệ: HACK_THE_WORLD", exception.getMessage());
        verify(rolePermissionRepository, never()).deleteAll(any());
        verify(rolePermissionRepository, never()).save(any(RolePermission.class));
    }

    @Test
    void update_role_permissions_role_not_found_shouldThrowException() {
        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
        request.setPermissionCodes(List.of(Permissions.CREATE_BOOKING));

        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> accessControlService.updateRolePermissions(999L, request));

        assertEquals("Không tìm thấy role có id = 999", exception.getMessage());
        verify(permissionRepository, never()).findByCode(anyString());
        verify(rolePermissionRepository, never()).deleteAll(any());
    }

    @Test
    void update_admin_permissions_shouldThrowException() {
        Role adminRole = createRole(3L, Roles.ADMIN);

        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
        request.setPermissionCodes(List.of(Permissions.VIEW_USERS));

        when(roleRepository.findById(3L)).thenReturn(Optional.of(adminRole));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> accessControlService.updateRolePermissions(3L, request));

        assertEquals("Không thể thay đổi quyền của ADMIN", exception.getMessage());
        verify(permissionRepository, never()).findByCode(anyString());
        verify(rolePermissionRepository, never()).deleteAll(any());
    }
}