package com.sporthub.controller;

import com.sporthub.dto.request.CreateAdminUserRequest;
import com.sporthub.dto.request.UpdateAdminUserRequest;
import com.sporthub.dto.request.UpdateUserRolesRequest;
import com.sporthub.dto.request.UpdateUserStatusRequest;
import com.sporthub.dto.response.UserResponse;
import com.sporthub.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).VIEW_USERS)")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page phải lớn hơn hoặc bằng 0")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size phải lớn hơn hoặc bằng 1")
            @Max(value = 100, message = "Size không được vượt quá 100")
            int size) {

        return ResponseEntity.ok(
                userService.getUsers(keyword, page, size)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).VIEW_USERS)")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable
            @Positive(message = "ID phải lớn hơn 0")
            Long id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).CREATE_USER)")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateAdminUserRequest request) {

        return ResponseEntity.ok(
                userService.createUser(request)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_USER)")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable
            @Positive(message = "ID phải lớn hơn 0")
            Long id,
            @Valid @RequestBody UpdateAdminUserRequest request) {

        return ResponseEntity.ok(
                userService.updateUser(id, request)
        );
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_USER_STATUS)")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable
            @Positive(message = "ID phải lớn hơn 0")
            Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        return ResponseEntity.ok(
                userService.updateStatus(id, request)
        );
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_USER_ROLES)")
    public ResponseEntity<UserResponse> updateRoles(
            @PathVariable
            @Positive(message = "ID phải lớn hơn 0")
            Long id,
            @Valid @RequestBody UpdateUserRolesRequest request) {

        return ResponseEntity.ok(
                userService.updateRoles(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).DELETE_USER)")
    public ResponseEntity<Void> deleteUser(
            @PathVariable
            @Positive(message = "ID phải lớn hơn 0")
            Long id) {

        userService.deactivateUser(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(
            value = "/{id}/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAuthority(T(com.sporthub.constant.Permissions).UPDATE_USER)")
    public ResponseEntity<UserResponse> updateAvatar(
            @PathVariable
            @Positive(message = "ID phải lớn hơn 0")
            Long id,
            @RequestPart("file") MultipartFile file) {

        return ResponseEntity.ok(
                userService.updateUserAvatar(id, file)
        );
    }
}