package com.sporthub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueRequest {

    @NotBlank(message = "Tên cơ sở không được để trống")
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String district;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String imageUrl;
}
