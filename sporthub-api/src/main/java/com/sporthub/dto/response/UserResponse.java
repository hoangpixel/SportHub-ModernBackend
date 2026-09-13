package com.sporthub.dto.response;

import com.sporthub.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;

    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String avatarUrl;
    private String address;
    private String city;

    private UserStatus status;

    private List<String> roles;
    private List<String> permissions;
}