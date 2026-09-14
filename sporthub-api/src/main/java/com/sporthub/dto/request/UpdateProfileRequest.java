package com.sporthub.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(
        max = 150,
        message = "Họ tên không được vượt quá 150 ký tự"
    )
    private String fullName;

    @Pattern(
        regexp = "^$|^\\+?[0-9]{9,15}$",
        message = "Số điện thoại không hợp lệ"
    )
    private String phone;

    @Size(
        max = 20,
        message = "Giới tính không được vượt quá 20 ký tự"
    )
    private String gender;

    @Past(
        message = "Ngày sinh phải nhỏ hơn ngày hiện tại"
    )
    private LocalDate dateOfBirth;

    @Size(
        max = 500,
        message = "Avatar URL không được vượt quá 500 ký tự"
    )
    private String avatarUrl;

    @Size(
        max = 255,
        message = "Địa chỉ không được vượt quá 255 ký tự"
    )
    private String address;

    @Size(
        max = 100,
        message = "Thành phố không được vượt quá 100 ký tự"
    )
    private String city;
}