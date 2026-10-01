package com.sporthub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminPermissionResponse {

    private Long id;
    private String code;
    private String description;
}