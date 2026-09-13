package com.sporthub.controller;

import com.sporthub.dto.request.LoginRequest;
import com.sporthub.dto.request.RegisterRequest;
import com.sporthub.dto.response.LoginResponse;
import com.sporthub.dto.response.RegisterResponse;
import com.sporthub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

        @PostMapping("/register")
        public ResponseEntity<RegisterResponse> register(
                @Valid @RequestBody RegisterRequest request) {

                RegisterResponse response =
                        authService.register(request);

                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(response);
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(
                @Valid @RequestBody LoginRequest request) {

        LoginResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
        }
}