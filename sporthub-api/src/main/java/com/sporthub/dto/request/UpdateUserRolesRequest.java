package com.sporthub.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRolesRequest {

    @NotEmpty(message = "User phải có ít nhất một role")
    private Set<String> roles;
}