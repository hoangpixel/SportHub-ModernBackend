package com.sporthub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;

    private String tokenType;

    private Long userId;

    private String username;

    private List<String> roles;

    private List<String> permissions;
}