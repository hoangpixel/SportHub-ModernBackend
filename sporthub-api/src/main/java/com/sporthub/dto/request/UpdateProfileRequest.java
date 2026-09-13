package com.sporthub.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProfileRequest {

    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String avatarUrl;
    private String address;
    private String city;
}