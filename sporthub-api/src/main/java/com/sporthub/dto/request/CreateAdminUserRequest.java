package com.sporthub.dto.request;

import com.sporthub.enums.UserStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class CreateAdminUserRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 50, message = "Username phải từ 4 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username chỉ được chứa chữ cái và số")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 8, message = "Password phải có ít nhất 8 ký tự")
    private String password;

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

    @NotNull(message = "Trạng thái không được để trống")
    private UserStatus status = UserStatus.ACTIVE;

    @NotEmpty(message = "User phải có ít nhất một role")
    private Set<String> roles;
}