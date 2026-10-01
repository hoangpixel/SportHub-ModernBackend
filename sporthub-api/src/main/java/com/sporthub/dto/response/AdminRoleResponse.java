package com.sporthub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminRoleResponse {

    private Long id;
    private String name;
    private long userCount;
    private List<String> permissions;
}