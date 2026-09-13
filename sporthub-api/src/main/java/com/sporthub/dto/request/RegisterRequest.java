package com.sporthub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 50,
          message = "Username phải từ 4 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 8,
          message = "Password phải có ít nhất 8 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
}