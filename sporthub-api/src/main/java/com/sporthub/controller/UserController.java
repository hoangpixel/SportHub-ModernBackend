package com.sporthub.controller;

import com.sporthub.dto.request.ChangePasswordRequest;
import com.sporthub.dto.request.UpdateProfileRequest;
import com.sporthub.dto.response.UserResponse;
import com.sporthub.service.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication) {

        return ResponseEntity.ok(
                userService.getCurrentUser(
                        authentication.getName()
                )
        );
    }

        @PutMapping("/me")
        public ResponseEntity<UserResponse> updateProfile(
                Authentication authentication,
                @Valid @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(
                userService.updateProfile(
                        authentication.getName(),
                        request
                )
        );
        }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid
            @RequestBody ChangePasswordRequest request) {

        userService.changePassword(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Đổi mật khẩu thành công"
                )
        );
    }

    @PutMapping(
        value = "/me/avatar",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
        )
        public ResponseEntity<UserResponse> updateAvatar(
                Authentication authentication,
                @RequestPart("file")
                MultipartFile file) {

        return ResponseEntity.ok(
                userService.updateAvatar(
                        authentication.getName(),
                        file
                )
        );
        }
}