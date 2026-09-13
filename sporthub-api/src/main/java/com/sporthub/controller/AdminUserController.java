package com.sporthub.controller;

import com.sporthub.constant.Permissions;
import com.sporthub.dto.request.UpdateUserRolesRequest;
import com.sporthub.dto.request.UpdateUserStatusRequest;
import com.sporthub.dto.response.UserResponse;
import com.sporthub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).VIEW_USERS)"
    )
    public ResponseEntity<Page<UserResponse>> getUsers(

            @RequestParam(defaultValue = "")
            String keyword,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size) {

        return ResponseEntity.ok(
                userService.getUsers(
                        keyword,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).VIEW_USERS)"
    )
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    @PutMapping("/{id}/status")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_USER_STATUS)"
    )
    public ResponseEntity<UserResponse> updateStatus(

            @PathVariable Long id,

            @Valid
            @RequestBody UpdateUserStatusRequest request) {

        return ResponseEntity.ok(
                userService.updateStatus(
                        id,
                        request
                )
        );
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize(
        "hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_USER_ROLES)"
    )
    public ResponseEntity<UserResponse> updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request) {

        return ResponseEntity.ok(
                userService.updateRoles(
                        id,
                        request
                )
        );
    }
}