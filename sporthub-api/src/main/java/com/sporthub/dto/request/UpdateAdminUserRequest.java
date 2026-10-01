package com.sporthub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateAdminUserRequest {

    @Size(min = 4, max = 50, message = "Username phải từ 4 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username chỉ được chứa chữ cái và số")
    private String username;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 20, message = "Giới tính không được vượt quá 20 ký tự")
    private String gender;

    @Past(message = "Ngày sinh phải nhỏ hơn ngày hiện tại")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Size(max = 100, message = "Thành phố không được vượt quá 100 ký tự")
    private String city;
}