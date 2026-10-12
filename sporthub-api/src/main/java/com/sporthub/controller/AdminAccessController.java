package com.sporthub.controller;

import com.sporthub.dto.request.UpdateRolePermissionsRequest;
import com.sporthub.dto.response.AdminPermissionResponse;
import com.sporthub.dto.response.AdminRoleResponse;
import com.sporthub.service.AccessControlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/access")
@RequiredArgsConstructor
@Validated
public class AdminAccessController {

    private final AccessControlService accessControlService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).VIEW_ROLES)")
    public ResponseEntity<List<AdminRoleResponse>> getRoles() {
        return ResponseEntity.ok(accessControlService.getRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).VIEW_PERMISSIONS)")
    public ResponseEntity<List<AdminPermissionResponse>> getPermissions() {
        return ResponseEntity.ok(accessControlService.getPermissions());
    }

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).MANAGE_ROLE_PERMISSIONS)")
    public ResponseEntity<AdminRoleResponse> updateRolePermissions(@PathVariable @Positive(message = "ID role phải lớn hơn 0") Long roleId, @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(accessControlService.updateRolePermissions(roleId, request));
    }
}