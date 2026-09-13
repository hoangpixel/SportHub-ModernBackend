package com.sporthub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SportRequest {

    @NotBlank(message = "Tên môn thể thao không được để trống")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String iconUrl;
}