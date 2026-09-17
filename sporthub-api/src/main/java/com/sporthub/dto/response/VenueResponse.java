package com.sporthub.dto.response;

import com.sporthub.enums.VenueStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class VenueResponse {

    private Long id;

    private String name;

    private String description;

    private String address;

    private String city;

    private String district;

    private String phone;

    private String imageUrl;

    private VenueStatus status;

    private Long ownerId;

    private String ownerUsername;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
