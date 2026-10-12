package com.sporthub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateRolePermissionsRequest {

    @NotNull(message = "Danh sách quyền không được để null")
    private List<@NotBlank(message = "Mã quyền không được để trống") String> permissionCodes;
}