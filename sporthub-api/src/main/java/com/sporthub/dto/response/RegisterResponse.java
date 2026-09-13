package com.sporthub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
}